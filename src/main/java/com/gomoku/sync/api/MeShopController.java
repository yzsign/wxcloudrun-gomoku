package com.gomoku.sync.api;

import com.gomoku.sync.api.dto.ShopCatalogPageResponse;
import com.gomoku.sync.api.dto.ShopCatalogResponse;
import com.gomoku.sync.service.ShopPricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * 无参：全量商品（与旧版一致，供客户端价表全量覆盖）。<br>
     * 有 page 与 size：分页，供杂货铺弹窗；仍返回全量 orderItemCodes 以同步下标。
     */
    @GetMapping("/shop/catalog")
    public ResponseEntity<?> catalog(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        if (page == null && size == null) {
            return ResponseEntity.ok(shopPricingService.buildCatalog());
        }
        if (page == null || size == null) {
            return ResponseEntity.badRequest().body("page and size must be both set or both omitted");
        }
        ShopCatalogPageResponse body = shopPricingService.buildCatalogPage(page, size);
        return ResponseEntity.ok(body);
    }
}
