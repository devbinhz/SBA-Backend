package com.bookverse.service.refund;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.refund.ApproveRefundRequestDTO;
import com.bookverse.dto.request.refund.CompleteInspectionRequestDTO;
import com.bookverse.dto.request.refund.CreateRefundRequestDTO;
import com.bookverse.dto.request.refund.RejectRefundRequestDTO;
import com.bookverse.dto.request.refund.SubmitReturnShipmentRequestDTO;
import com.bookverse.dto.response.refund.RefundRequestResponseDTO;
import com.bookverse.enums.RefundStatus;
import com.bookverse.enums.UserRole;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RefundRequestService {

    RefundRequestResponseDTO createRequest(Long customerId, Long orderId, CreateRefundRequestDTO request);

    RefundRequestResponseDTO getLatestForOrder(Long currentUserId, UserRole currentUserRole, Long orderId);

    PageResponseDTO<RefundRequestResponseDTO> listForAdmin(RefundStatus status, List<RefundStatus> statuses, Pageable pageable);

    RefundRequestResponseDTO getForAdmin(Long id);

    RefundRequestResponseDTO approve(Long adminId, Long id, ApproveRefundRequestDTO request);

    RefundRequestResponseDTO reject(Long adminId, Long id, RejectRefundRequestDTO request);

    RefundRequestResponseDTO submitReturnShipment(Long customerId, Long orderId, Long id, SubmitReturnShipmentRequestDTO request);

    RefundRequestResponseDTO confirmReceived(Long adminId, Long id);

    RefundRequestResponseDTO startInspection(Long adminId, Long id);

    RefundRequestResponseDTO completeInspection(Long adminId, Long id, CompleteInspectionRequestDTO request);

    RefundRequestResponseDTO markRefundProcessed(Long adminId, Long id);

    RefundRequestResponseDTO closeRequest(Long adminId, Long id);
}
