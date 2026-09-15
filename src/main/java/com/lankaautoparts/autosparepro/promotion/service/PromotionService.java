package com.lankaautoparts.autosparepro.promotion.service;

import com.lankaautoparts.autosparepro.promotion.model.Promotion;
import com.lankaautoparts.autosparepro.promotion.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PromotionService {
    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    public Promotion savePromotion(Promotion promotion) {
        return promotionRepository.save(promotion);
    }
}
