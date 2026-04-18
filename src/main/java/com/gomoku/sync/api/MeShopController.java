package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ShopCatalogResponse;
import com.gomoku.sync.service.ShopPricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 杂货铺目录与定价（读库；未登录也可拉取以展示价格，兑换仍须登录）。
 */
@RestController
@RequestMapping("/api/me")
public class MeShopController {

    private final ShopPricingService shopPricingService;

    public MeShopController(ShopPricingService shopPricingService) {
        this.shopPricingService = shopPricingService;
    }

    @GetMapping("/shop/catalog")
    public ResponseEntity<ShopCatalogResponse> catalog() {
        return ResponseEntity.ok(shopPricingService.buildCatalog());
    }
}
