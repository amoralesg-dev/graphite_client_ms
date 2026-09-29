package com.rassini.graphite_client.service.resolver;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ErpResolutionResult {
    private final String resolvedErpId;
    private final ErpResolutionStrategy strategy;
}
