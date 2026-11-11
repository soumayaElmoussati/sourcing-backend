package app_sourcing.sourcingModule.service;

import app_sourcing.sourcingModule.dtos.SourcingResponseDTO;
import app_sourcing.sourcingModule.dtos.SourcingStatsDto;
import app_sourcing.sourcingModule.entitie.RequestStatus;
import app_sourcing.sourcingModule.entitie.SourcingRequest;
import app_sourcing.sourcingModule.mappers.SourcingRequestMapper;
import app_sourcing.sourcingModule.repo.SourcingRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SourcingRequestGetDataService {
    private final SourcingRequestRepository sourcingRequestRepository;


//    public Page<SourcingRequest> getSellerRequests(Long sellerId, Pageable pageable) {
//     //   return sourcingRequestRepository.findBySellerId(sellerId);
//        return sourcingRequestRepository.findBySellerId(sellerId, pageable);
//    }

    public Page<SourcingRequest> getSellerRequests(Long sellerId,Pageable pageable) {

        pageable=PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return sourcingRequestRepository.findBySellerId(sellerId, pageable);
    }

    public Page<SourcingResponseDTO> getAllRequests(Pageable pageable) {

        pageable=PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<SourcingRequest> requestsPage = sourcingRequestRepository.findAll(pageable);

        return requestsPage.map(SourcingRequestMapper::toResponseDTO);
    }


    public SourcingStatsDto getStats() {

        long pending = sourcingRequestRepository.countByStatus(RequestStatus.PENDING);
        long approved = sourcingRequestRepository.countByStatus(RequestStatus.APPROVED);
        long completed = sourcingRequestRepository.countByStatus(RequestStatus.COMPLETED);

        BigDecimal totalAmount = sourcingRequestRepository.sumTotalAmount();

        return new SourcingStatsDto(
                pending,
                approved,
                completed,
                totalAmount
        );
    }

    public SourcingRequest getRequestById(String id) {
        return sourcingRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sourcing request not found"));
    }

}
