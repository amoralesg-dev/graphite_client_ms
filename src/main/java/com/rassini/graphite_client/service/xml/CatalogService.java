package com.rassini.graphite_client.service.xml;

public interface CatalogService {
  String mapState(String graphiteState, String plantId);
  String mapCountry(String graphiteCountry, String plantId);
  String mapCurrency(String graphiteCurrency, String plantId);

  CatalogService.GlProfile resolveGlProfile(String plantId, String currency, boolean isForeign);
  String resolveTaxClass(String plantId, String taxClass);
  String resolvePaymentTerms(String plantId, String terms);
  String resolvePurchaseType(String plantId, String paymentType);
  String resolveSupplierType(String plantId, String supplierType);

  record GlProfile(String invControl, String cnControl, String prepayControl, String divProfile, String purchaseGlProfile) {}
}
