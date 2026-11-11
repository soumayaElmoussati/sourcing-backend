package app_sourcing.walletModule.controller;

import app_sourcing.walletModule.dtos.FeeDTO;
import app_sourcing.walletModule.service.FeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/fees")
@RequiredArgsConstructor
public class SellerFeeController {

    private final FeeService feeService;

    /** Endpoint public pour les sellers — lecture seule des frais actifs */
    @GetMapping
    public List<FeeDTO> getFeesForSeller() {
        return feeService.getAllFees();
    }
}