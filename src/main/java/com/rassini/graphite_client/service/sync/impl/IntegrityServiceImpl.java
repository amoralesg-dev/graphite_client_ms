package com.rassini.graphite_client.service.sync.impl;

import org.springframework.stereotype.Service;
import com.rassini.graphite_client.service.sync.IntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrityServiceImpl implements IntegrityService {

    // Implementation of methods from IntegrityService interface will go here
    @Override
    public void createFileSupplierSync(){
        log.info("Executing createFileSupplierSync method");
    }
}