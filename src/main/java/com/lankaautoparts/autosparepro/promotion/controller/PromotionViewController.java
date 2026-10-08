package com.lankaautoparts.autosparepro.promotion.controller;

import com.lankaautoparts.autosparepro.inventory.model.PartCategory;
import com.lankaautoparts.autosparepro.promotion.model.Promotion;
import com.lankaautoparts.autosparepro.promotion.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;

@Controller
@RequestMapping("/promotions")
public class PromotionViewController {
    private final PromotionService promotionService;

    public PromotionViewController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    /**
     * The Category select allows a blank "Storewide" option so a
     * promotion can apply to every category. The default Spring enum
     * converter treats an empty submitted value as invalid input (a
     * typeMismatch binding error), so this treats blank as null instead.
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(PartCategory.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue((text == null || text.isBlank()) ? null : PartCategory.valueOf(text));
            }
        });
    }

    @GetMapping
    public String list(Model model) {
        addFormAttributes(model, null);
        model.addAttribute("promotion", new Promotion());
        return "promotion";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        addFormAttributes(model, id);
        model.addAttribute("promotion", promotionService.getPromotionById(id));
        return "promotion";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("promotion") Promotion promotion, BindingResult bindingResult, Model model) {
        validateDates(promotion, bindingResult);
        if (bindingResult.hasErrors()) {
            addFormAttributes(model, promotion.getId());
            return "promotion";
        }
        promotionService.savePromotion(promotion);
        return "redirect:/promotions";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return "redirect:/promotions";
    }

    /**
     * A promotion can't be scheduled in the past. When editing, a date that
     * already lay in the past and is being left unchanged is fine (the
     * promotion simply started/ended earlier) — only newly entered past
     * dates are rejected. The end date can never precede the start date.
     * The date pickers are locked to the same rule in the browser, but that
     * alone can be bypassed, so it is enforced here too.
     */
    private void validateDates(Promotion promotion, BindingResult bindingResult) {
        LocalDate today = LocalDate.now();
        LocalDate savedStart = null;
        LocalDate savedEnd = null;
        if (promotion.getId() != null) {
            Promotion saved = promotionService.getPromotionById(promotion.getId());
            savedStart = saved.getStartDate();
            savedEnd = saved.getEndDate();
        }

        LocalDate start = promotion.getStartDate();
        LocalDate end = promotion.getEndDate();

        if (start != null && start.isBefore(today) && !start.equals(savedStart)) {
            bindingResult.rejectValue("startDate", "startDate.past", "Start date can't be in the past.");
        }
        if (end != null && end.isBefore(today) && !end.equals(savedEnd)) {
            bindingResult.rejectValue("endDate", "endDate.past", "End date can't be in the past.");
        }
        if (start != null && end != null && end.isBefore(start)) {
            bindingResult.rejectValue("endDate", "endDate.beforeStart", "End date can't be before the start date.");
        }
    }

    /**
     * Everything the page needs besides the promotion itself: the lists, and
     * the date-picker locks. {@code startLocked}/{@code endPast} describe the
     * promotion as it is SAVED (not as typed), so a start date that has
     * already passed can't be moved, and an already-past end date doesn't
     * block the browser from submitting an unrelated edit.
     */
    private void addFormAttributes(Model model, Long editingId) {
        LocalDate today = LocalDate.now();
        boolean startLocked = false;
        boolean endPast = false;
        if (editingId != null) {
            Promotion saved = promotionService.getPromotionById(editingId);
            startLocked = saved.getStartDate() != null && saved.getStartDate().isBefore(today);
            endPast = saved.getEndDate() != null && saved.getEndDate().isBefore(today);
        }
        model.addAttribute("promotions", promotionService.getAllPromotions());
        model.addAttribute("categories", PartCategory.values());
        model.addAttribute("today", today);
        model.addAttribute("startLocked", startLocked);
        model.addAttribute("endPast", endPast);
    }
}
