package com.lankaautoparts.autosparepro.promotion.service;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.promotion.model.Promotion;
import com.lankaautoparts.autosparepro.promotion.repository.PromotionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PromotionService {
    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    public Promotion getPromotionById(Long id) {
        return promotionRepository.findById(id).orElse(new Promotion());
    }

    public Promotion savePromotion(Promotion promotion) {
        return promotionRepository.save(promotion);
    }

    public void deletePromotion(Long id) {
        promotionRepository.deleteById(id);
    }

    /**
     * Promotions that are "live" right now: today falls within their
     * start/end date window. A promotion with no start date is treated as
     * already started, and one with no end date is treated as open-ended.
     */
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findActiveOn(LocalDate.now());
    }

    /**
     * The best (highest) active discount percentage that applies to a given
     * part — either a promotion scoped to that part's category, or a
     * storewide promotion (category left blank). Returns 0 if nothing
     * applies right now.
     */
    public double getBestDiscountForPart(Part part) {
        return bestDiscount(getActivePromotions(), part);
    }

    /**
     * Best active discount for each of the given parts, keyed by part id -
     * parts with no applicable promotion are left out. Loads the active
     * promotions once for the whole list rather than once per part.
     */
    public Map<Long, Double> getBestDiscountsForParts(List<Part> parts) {
        List<Promotion> active = getActivePromotions();
        Map<Long, Double> discounts = new HashMap<>();
        for (Part part : parts) {
            double discount = bestDiscount(active, part);
            if (discount > 0) {
                discounts.put(part.getId(), discount);
            }
        }
        return discounts;
    }

    private static double bestDiscount(List<Promotion> active, Part part) {
        return active.stream()
                .filter(p -> p.getCategory() == null || p.getCategory() == part.getCategory())
                .mapToDouble(Promotion::getDiscountPercentage)
                .max()
                .orElse(0.0);
    }
}
