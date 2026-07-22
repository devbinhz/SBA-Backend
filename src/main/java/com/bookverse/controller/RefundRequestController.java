package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.refund.ApproveRefundRequestDTO;
import com.bookverse.dto.request.refund.CompleteInspectionRequestDTO;
import com.bookverse.dto.request.refund.CreateRefundRequestDTO;
import com.bookverse.dto.request.refund.RejectRefundRequestDTO;
import com.bookverse.dto.request.refund.SubmitEvidenceRequestDTO;
import com.bookverse.dto.request.refund.SubmitReturnShipmentRequestDTO;
import com.bookverse.dto.response.refund.RefundRequestResponseDTO;
import com.bookverse.enums.RefundStatus;
import com.bookverse.enums.UserRole;
import com.bookverse.service.refund.RefundRequestService;
import com.bookverse.service.upload.EvidenceUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Refund Requests", description = "Customer return/refund request and admin review APIs")
public class RefundRequestController {

    private final RefundRequestService refundRequestService;
    private final EvidenceUploadService evidenceUploadService;

    @PostMapping("/orders/{orderId}/refund-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create a return request for an order (Customer)")
    public ApiResponse<RefundRequestResponseDTO> createRefundRequest(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PathVariable Long orderId,
            @Valid @RequestBody CreateRefundRequestDTO request) {
        return ApiResponse.success(
                refundRequestService.createRequest(userId, orderId, request),
                "Return request submitted successfully"
        );
    }

    @GetMapping("/orders/{orderId}/refund-requests/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get the latest return request for an order (owner customer or admin)")
    public ApiResponse<RefundRequestResponseDTO> getMyRefundRequest(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @AuthenticationPrincipal(expression = "user.role") UserRole role,
            @PathVariable Long orderId) {
        return ApiResponse.success(refundRequestService.getLatestForOrder(userId, role, orderId));
    }

    @PostMapping("/refund-requests/evidence/upload")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Upload an image or video file for refund evidence (Customer)")
    public ApiResponse<Map<String, String>> uploadEvidenceFile(@RequestParam("file") MultipartFile file) throws IOException {
        String url = evidenceUploadService.upload(file);
        return ApiResponse.success(Map.of("url", url));
    }

    @PutMapping("/orders/{orderId}/refund-requests/{id}/evidence")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Submit supporting evidence for a return request (Customer)")
    public ApiResponse<RefundRequestResponseDTO> submitEvidence(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PathVariable Long orderId,
            @PathVariable Long id,
            @Valid @RequestBody SubmitEvidenceRequestDTO request) {
        return ApiResponse.success(refundRequestService.submitEvidence(userId, orderId, id, request), "Evidence submitted successfully");
    }

    @PutMapping("/orders/{orderId}/refund-requests/{id}/return-shipment")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Submit return shipping info for an approved return request (Customer)")
    public ApiResponse<RefundRequestResponseDTO> submitReturnShipment(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PathVariable Long orderId,
            @PathVariable Long id,
            @Valid @RequestBody SubmitReturnShipmentRequestDTO request) {
        return ApiResponse.success(
                refundRequestService.submitReturnShipment(userId, orderId, id, request),
                "Return shipment info submitted successfully"
        );
    }

    @GetMapping("/admin/refund-requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List return requests, optionally filtered by status (Admin)")
    public ApiResponse<PageResponseDTO<RefundRequestResponseDTO>> listRefundRequests(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) List<RefundStatus> statuses,
            Pageable pageable) {
        return ApiResponse.success(refundRequestService.listForAdmin(status, statuses, pageable));
    }

    @GetMapping("/admin/refund-requests/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get return request detail (Admin)")
    public ApiResponse<RefundRequestResponseDTO> getRefundRequest(@PathVariable Long id) {
        return ApiResponse.success(refundRequestService.getForAdmin(id));
    }

    @PutMapping("/admin/refund-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a return request and choose a resolution path (Admin)")
    public ApiResponse<RefundRequestResponseDTO> approveRefundRequest(
            @AuthenticationPrincipal(expression = "user.id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody ApproveRefundRequestDTO request) {
        return ApiResponse.success(refundRequestService.approve(adminId, id, request), "Return request approved");
    }

    @PutMapping("/admin/refund-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a return request (Admin)")
    public ApiResponse<RefundRequestResponseDTO> rejectRefundRequest(
            @AuthenticationPrincipal(expression = "user.id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody RejectRefundRequestDTO request) {
        return ApiResponse.success(refundRequestService.reject(adminId, id, request), "Return request rejected");
    }

    @PutMapping("/admin/refund-requests/{id}/confirm-received")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirm the returned item has been received at the warehouse (Admin)")
    public ApiResponse<RefundRequestResponseDTO> confirmReceived(
            @AuthenticationPrincipal(expression = "user.id") Long adminId,
            @PathVariable Long id) {
        return ApiResponse.success(refundRequestService.confirmReceived(adminId, id), "Return receipt confirmed");
    }

    @PutMapping("/admin/refund-requests/{id}/start-inspection")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Start warehouse inspection of the returned item (Admin)")
    public ApiResponse<RefundRequestResponseDTO> startInspection(
            @AuthenticationPrincipal(expression = "user.id") Long adminId,
            @PathVariable Long id) {
        return ApiResponse.success(refundRequestService.startInspection(adminId, id), "Inspection started");
    }

    @PutMapping("/admin/refund-requests/{id}/complete-inspection")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete warehouse inspection with a pass/fail outcome (Admin)")
    public ApiResponse<RefundRequestResponseDTO> completeInspection(
            @AuthenticationPrincipal(expression = "user.id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody CompleteInspectionRequestDTO request) {
        return ApiResponse.success(refundRequestService.completeInspection(adminId, id, request), "Inspection completed");
    }

    @PutMapping("/admin/refund-requests/{id}/replacement-shipment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Submit replacement/reshipment shipping info (Admin)")
    public ApiResponse<RefundRequestResponseDTO> submitReplacementShipment(
            @AuthenticationPrincipal(expression = "user.id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody SubmitReturnShipmentRequestDTO request) {
        return ApiResponse.success(refundRequestService.submitReplacementShipment(adminId, id, request), "Replacement shipment info submitted");
    }

    @PutMapping("/admin/refund-requests/{id}/mark-refund-processed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark the manual bank refund as processed (Admin)")
    public ApiResponse<RefundRequestResponseDTO> markRefundProcessed(
            @AuthenticationPrincipal(expression = "user.id") Long adminId,
            @PathVariable Long id) {
        return ApiResponse.success(refundRequestService.markRefundProcessed(adminId, id), "Refund marked as processed");
    }

    @PutMapping("/admin/refund-requests/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Close a completed return request (Admin)")
    public ApiResponse<RefundRequestResponseDTO> closeRefundRequest(
            @AuthenticationPrincipal(expression = "user.id") Long adminId,
            @PathVariable Long id) {
        return ApiResponse.success(refundRequestService.closeRequest(adminId, id), "Return request closed");
    }
}
