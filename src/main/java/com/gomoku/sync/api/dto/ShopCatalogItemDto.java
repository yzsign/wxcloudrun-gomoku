package com.gomoku.sync.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * GET /api/me/shop/catalog 单行：与 shop_items + 当前有效 shop_item_prices 一致。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopCatalogItemDto {

    private String itemCode;
    private String shopCategory;
    private String redeemMode;
    private String displayLabel;
    private int sortOrder;
    private String consumableKind;
    private String clientRowId;
    /** 无积分价时为空（如 FREE、CHECKIN_UNLOCK） */
    private Integer priceAmount;
    private String currency;
    private String unitType;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getShopCategory() {
        return shopCategory;
    }

    public void setShopCategory(String shopCategory) {
        this.shopCategory = shopCategory;
    }

    public String getRedeemMode() {
        return redeemMode;
    }

    public void setRedeemMode(String redeemMode) {
        this.redeemMode = redeemMode;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getConsumableKind() {
        return consumableKind;
    }

    public void setConsumableKind(String consumableKind) {
        this.consumableKind = consumableKind;
    }

    public String getClientRowId() {
        return clientRowId;
    }

    public void setClientRowId(String clientRowId) {
        this.clientRowId = clientRowId;
    }

    public Integer getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(Integer priceAmount) {
        this.priceAmount = priceAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }
}
