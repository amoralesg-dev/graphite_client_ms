package com.rassini.graphite_client.service.xml;

import com.rassini.graphite_client.entity.SuppliersRowEntity;

public interface CatalogService {
  String getEquivalenciaState(String graphiteState, String plantId);
  String mapCountry(String graphiteCountry, String plantId);
  String mapCurrency(String graphiteCurrency, String plantId);

  CatalogService.GlProfile resolveGlProfile(String plantId, String currency, boolean isForeign);
  String resolveTaxClass(String plantId, String taxClass);
  String resolvePaymentTerms(String plantId, String terms);
  String resolvePurchaseType(String plantId, String paymentType);
  String resolveSupplierType(String plantId, String supplierType);

  String getActivityCode(SuppliersRowEntity supplier);

  record GlProfile(String invControl, String cnControl, String prepayControl, String divProfile, String purchaseGlProfile) {}

  String getAction(SuppliersRowEntity supplier);
  String getPartialUpdate(SuppliersRowEntity supplier);
}
