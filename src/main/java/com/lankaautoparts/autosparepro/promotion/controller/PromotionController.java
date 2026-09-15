package com.lankaautoparts.autosparepro.promotion.controller;

import com.lankaautoparts.autosparepro.promotion.model.Promotion;
import com.lankaautoparts.autosparepro.promotion.service.PromotionService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {
    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public List<Promotion> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @PostMapping
    public Promotion addPromotion(@RequestBody Promotion promotion) {
        return promotionService.savePromotion(promotion);
    }
}
