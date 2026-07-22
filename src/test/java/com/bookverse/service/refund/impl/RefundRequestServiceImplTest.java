package com.bookverse.service.refund.impl;

import com.bookverse.common.exception.BadRequestException;
import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ForbiddenException;
import com.bookverse.dto.request.refund.ApproveRefundRequestDTO;
import com.bookverse.dto.request.refund.CompleteInspectionRequestDTO;
import com.bookverse.dto.request.refund.CreateRefundRequestDTO;
import com.bookverse.dto.request.refund.RefundItemSelectionDTO;
import com.bookverse.dto.request.refund.RejectRefundRequestDTO;
import com.bookverse.dto.request.refund.SubmitReturnShipmentRequestDTO;
import com.bookverse.dto.response.refund.RefundRequestResponseDTO;
import com.bookverse.entity.Order;
import com.bookverse.entity.OrderItem;
import com.bookverse.entity.RefundEvidence;
import com.bookverse.entity.RefundRequest;
import com.bookverse.entity.RefundRequestItem;
import com.bookverse.entity.User;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.RefundReason;
import com.bookverse.enums.RefundStatus;
import com.bookverse.mapper.RefundRequestMapper;
import com.bookverse.repository.OrderItemRepository;
import com.bookverse.repository.OrderRepository;
import com.bookverse.repository.RefundEvidenceRepository;
import com.bookverse.repository.RefundRequestItemRepository;
import com.bookverse.repository.RefundRequestRepository;
import com.bookverse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundRequestServiceImplTest {

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private RefundRequestItemRepository refundRequestItemRepository;

    @Mock
    private RefundEvidenceRepository refundEvidenceRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefundRequestMapper refundRequestMapper;

    @InjectMocks
    private RefundRequestServiceImpl refundRequestService;

    private User customer;
    private User admin;
    private Order order;
    private OrderItem orderItem;
    private CreateRefundRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(1L).fullName("BookVerse Customer").build();
        admin = User.builder().id(9L).fullName("BookVerse Admin").build();
        order = Order.builder().id(50L).user(customer).status(OrderStatus.DELIVERED)
                .deliveredAt(Instant.now().minus(2, ChronoUnit.DAYS)).total(300000L).build();
        orderItem = OrderItem.builder().id(500L).order(order).titleSnapshot("Clean Code").unitPrice(150000L).quantity(2).lineTotal(300000L).build();
        createRequest = new CreateRefundRequestDTO();
        createRequest.setItems(List.of(selection(500L, 1)));
        createRequest.setReason(RefundReason.WRONG_BOOK);
        createRequest.setDescription("Wrong book delivered");
        createRequest.setBankName("Test Bank");
        createRequest.setBankAccountNumber("123456789");
        createRequest.setBankAccountHolder("TEST USER");
        createRequest.setEvidenceUrls(List.of("https://example.com/evidence1.jpg", "https://example.com/evidence2.mp4"));

        lenient().when(refundRequestItemRepository.findByRefundRequestIdOrderByIdAsc(any())).thenReturn(List.of());
        lenient().when(refundEvidenceRepository.findByRefundRequestIdOrderByIdAsc(any())).thenReturn(List.of());
        lenient().when(refundRequestMapper.toResponse(any(RefundRequest.class), any(), any()))
                .thenReturn(RefundRequestResponseDTO.builder().build());
    }

    private static RefundItemSelectionDTO selection(Long orderItemId, int quantity) {
        RefundItemSelectionDTO selection = new RefundItemSelectionDTO();
        selection.setOrderItemId(orderItemId);
        selection.setQuantity(quantity);
        return selection;
    }

    // ---------- createRequest ----------

    @Test
    void createRequest_success() {
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(50L)).thenReturn(List.of(orderItem));
        when(refundRequestItemRepository.existsActiveForOrderItem(eq(500L), any())).thenReturn(false);
        when(refundRequestItemRepository.sumQuantityByOrderItemIdExcludingRejected(500L)).thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> {
            RefundRequest r = i.getArgument(0);
            r.setId(200L);
            return r;
        });
        when(refundRequestItemRepository.save(any(RefundRequestItem.class))).thenAnswer(i -> i.getArgument(0));
        when(refundRequestMapper.toResponse(any(RefundRequest.class), any(), any()))
                .thenReturn(RefundRequestResponseDTO.builder().id(200L).status(RefundStatus.UNDER_REVIEW).requestedAmount(150000L).build());

        RefundRequestResponseDTO response = refundRequestService.createRequest(1L, 50L, createRequest);

        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getRequestedAmount()).isEqualTo(150000L);
        ArgumentCaptor<RefundRequest> captor = ArgumentCaptor.forClass(RefundRequest.class);
        verify(refundRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestedAmount()).isEqualTo(150000L);
        assertThat(captor.getValue().getStatus()).isEqualTo(RefundStatus.UNDER_REVIEW);
        ArgumentCaptor<RefundRequestItem> itemCaptor = ArgumentCaptor.forClass(RefundRequestItem.class);
        verify(refundRequestItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getOrderItem()).isEqualTo(orderItem);
        assertThat(itemCaptor.getValue().getQuantity()).isEqualTo(1);
        ArgumentCaptor<RefundEvidence> evidenceCaptor = ArgumentCaptor.forClass(RefundEvidence.class);
        verify(refundEvidenceRepository, times(2)).save(evidenceCaptor.capture());
        assertThat(evidenceCaptor.getAllValues()).extracting(RefundEvidence::getUrl)
                .containsExactly("https://example.com/evidence1.jpg", "https://example.com/evidence2.mp4");
    }

    @Test
    void createRequest_withInsufficientEvidence_throwsBadRequest() {
        createRequest.setEvidenceUrls(List.of("https://example.com/evidence1.jpg"));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> refundRequestService.createRequest(1L, 50L, createRequest))
                .isInstanceOf(BadRequestException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_whenNotOwner_throwsForbidden() {
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> refundRequestService.createRequest(999L, 50L, createRequest))
                .isInstanceOf(ForbiddenException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_whenOrderNotDelivered_throwsConflict() {
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> refundRequestService.createRequest(1L, 50L, createRequest))
                .isInstanceOf(ConflictException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_whenDuplicateItemInSelection_throwsBadRequest() {
        createRequest.setItems(List.of(selection(500L, 1), selection(500L, 1)));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> refundRequestService.createRequest(1L, 50L, createRequest))
                .isInstanceOf(BadRequestException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_whenSelectedItemNotInOrder_throwsBadRequest() {
        createRequest.setItems(List.of(selection(999L, 1)));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(50L)).thenReturn(List.of(orderItem));

        assertThatThrownBy(() -> refundRequestService.createRequest(1L, 50L, createRequest))
                .isInstanceOf(BadRequestException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_whenQuantityExceedsPurchased_throwsBadRequest() {
        createRequest.setItems(List.of(selection(500L, 3)));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(50L)).thenReturn(List.of(orderItem));

        assertThatThrownBy(() -> refundRequestService.createRequest(1L, 50L, createRequest))
                .isInstanceOf(BadRequestException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_whenActiveRequestExistsForItem_throwsConflict() {
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(50L)).thenReturn(List.of(orderItem));
        when(refundRequestItemRepository.existsActiveForOrderItem(eq(500L), any())).thenReturn(true);

        assertThatThrownBy(() -> refundRequestService.createRequest(1L, 50L, createRequest))
                .isInstanceOf(ConflictException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_whenRemainingQuantityExceeded_throwsBadRequest() {
        // orderItem quantity=2, 2 already used across earlier non-rejected requests, requesting 1 more -> exceeds
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(50L)).thenReturn(List.of(orderItem));
        when(refundRequestItemRepository.existsActiveForOrderItem(eq(500L), any())).thenReturn(false);
        when(refundRequestItemRepository.sumQuantityByOrderItemIdExcludingRejected(500L)).thenReturn(2L);

        assertThatThrownBy(() -> refundRequestService.createRequest(1L, 50L, createRequest))
                .isInstanceOf(BadRequestException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    // ---------- approve ----------

    @Test
    void approve_success_movesToPickupPending() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer)
                .reason(RefundReason.BOOK_DEFECT).status(RefundStatus.UNDER_REVIEW).build();
        ApproveRefundRequestDTO request = new ApproveRefundRequestDTO();
        request.setNote("Approved after evidence review");

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(refundRequestRepository.save(refundRequest)).thenReturn(refundRequest);

        refundRequestService.approve(9L, 200L, request);

        assertThat(refundRequest.getStatus()).isEqualTo(RefundStatus.PICKUP_PENDING);
        assertThat(refundRequest.getDecisionNote()).isEqualTo("Approved after evidence review");
        assertThat(refundRequest.getDecidedBy()).isEqualTo(admin);
    }

    @Test
    void approve_whenNotUnderReview_throwsConflict() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer)
                .reason(RefundReason.MISSING_BOOK).status(RefundStatus.PICKUP_PENDING).build();
        ApproveRefundRequestDTO request = new ApproveRefundRequestDTO();

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));

        assertThatThrownBy(() -> refundRequestService.approve(9L, 200L, request))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- reject ----------

    @Test
    void reject_success() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.UNDER_REVIEW).build();
        RejectRefundRequestDTO request = new RejectRefundRequestDTO();
        request.setNote("Insufficient evidence");

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(refundRequestRepository.save(refundRequest)).thenReturn(refundRequest);

        refundRequestService.reject(9L, 200L, request);

        assertThat(refundRequest.getStatus()).isEqualTo(RefundStatus.REJECTED);
        assertThat(refundRequest.getDecisionNote()).isEqualTo("Insufficient evidence");
        assertThat(refundRequest.getDecidedBy()).isEqualTo(admin);
    }

    @Test
    void reject_whenNotUnderReview_throwsConflict() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.PICKUP_PENDING).build();
        RejectRefundRequestDTO request = new RejectRefundRequestDTO();
        request.setNote("Too late");

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));

        assertThatThrownBy(() -> refundRequestService.reject(9L, 200L, request))
                .isInstanceOf(ConflictException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    // ---------- submitReturnShipment ----------

    @Test
    void submitReturnShipment_success_doesNotChangeStatus() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.PICKUP_PENDING).build();
        SubmitReturnShipmentRequestDTO shipment = new SubmitReturnShipmentRequestDTO();
        shipment.setShippingProvider("GHTK");
        shipment.setTrackingCode("TRACK-001");

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));
        when(refundRequestRepository.save(refundRequest)).thenReturn(refundRequest);

        refundRequestService.submitReturnShipment(1L, 50L, 200L, shipment);

        assertThat(refundRequest.getStatus()).isEqualTo(RefundStatus.PICKUP_PENDING);
        assertThat(refundRequest.getReturnShippingProvider()).isEqualTo("GHTK");
        assertThat(refundRequest.getReturnTrackingCode()).isEqualTo("TRACK-001");
        assertThat(refundRequest.getReturnShippedAt()).isNotNull();
    }

    @Test
    void submitReturnShipment_whenNotOwner_throwsForbidden() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.PICKUP_PENDING).build();
        SubmitReturnShipmentRequestDTO shipment = new SubmitReturnShipmentRequestDTO();
        shipment.setShippingProvider("GHTK");
        shipment.setTrackingCode("TRACK-001");

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));

        assertThatThrownBy(() -> refundRequestService.submitReturnShipment(999L, 50L, 200L, shipment))
                .isInstanceOf(ForbiddenException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void submitReturnShipment_whenNotPickupPending_throwsConflict() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.UNDER_REVIEW).build();
        SubmitReturnShipmentRequestDTO shipment = new SubmitReturnShipmentRequestDTO();
        shipment.setShippingProvider("GHTK");
        shipment.setTrackingCode("TRACK-001");

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));

        assertThatThrownBy(() -> refundRequestService.submitReturnShipment(1L, 50L, 200L, shipment))
                .isInstanceOf(ConflictException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    // ---------- confirmReceived ----------

    @Test
    void confirmReceived_success() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.PICKUP_PENDING).build();

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(refundRequestRepository.save(refundRequest)).thenReturn(refundRequest);

        refundRequestService.confirmReceived(9L, 200L);

        assertThat(refundRequest.getStatus()).isEqualTo(RefundStatus.RETURN_RECEIVED);
        assertThat(refundRequest.getReceivedBy()).isEqualTo(admin);
        assertThat(refundRequest.getReceivedAt()).isNotNull();
    }

    @Test
    void confirmReceived_whenNotPickupPending_throwsConflict() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.RETURN_RECEIVED).build();

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));

        assertThatThrownBy(() -> refundRequestService.confirmReceived(9L, 200L))
                .isInstanceOf(ConflictException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    // ---------- startInspection ----------

    @Test
    void startInspection_success() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.RETURN_RECEIVED).build();

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(refundRequestRepository.save(refundRequest)).thenReturn(refundRequest);

        refundRequestService.startInspection(9L, 200L);

        assertThat(refundRequest.getStatus()).isEqualTo(RefundStatus.INSPECTING);
        assertThat(refundRequest.getInspectedBy()).isEqualTo(admin);
        assertThat(refundRequest.getInspectionStartedAt()).isNotNull();
    }

    @Test
    void startInspection_whenNotReturnReceived_throwsConflict() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.PICKUP_PENDING).build();

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));

        assertThatThrownBy(() -> refundRequestService.startInspection(9L, 200L))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- completeInspection ----------

    @Test
    void completeInspection_failed_requiresNote_movesToRejected() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer)
                .status(RefundStatus.INSPECTING).build();
        CompleteInspectionRequestDTO request = new CompleteInspectionRequestDTO();
        request.setPassed(false);
        request.setNote("Book was used and damaged");

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));
        when(refundRequestRepository.save(refundRequest)).thenReturn(refundRequest);

        refundRequestService.completeInspection(9L, 200L, request);

        assertThat(refundRequest.getStatus()).isEqualTo(RefundStatus.REJECTED);
        assertThat(refundRequest.getInspectionPassed()).isFalse();
    }

    @Test
    void completeInspection_failedWithoutNote_throwsBadRequest() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer)
                .status(RefundStatus.INSPECTING).build();
        CompleteInspectionRequestDTO request = new CompleteInspectionRequestDTO();
        request.setPassed(false);

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));

        assertThatThrownBy(() -> refundRequestService.completeInspection(9L, 200L, request))
                .isInstanceOf(BadRequestException.class);

        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void completeInspection_passed_movesToRefundProcessing() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer)
                .status(RefundStatus.INSPECTING).build();
        CompleteInspectionRequestDTO request = new CompleteInspectionRequestDTO();
        request.setPassed(true);

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));
        when(refundRequestRepository.save(refundRequest)).thenReturn(refundRequest);

        refundRequestService.completeInspection(9L, 200L, request);

        assertThat(refundRequest.getStatus()).isEqualTo(RefundStatus.REFUND_PROCESSING);
    }

    @Test
    void completeInspection_whenNotInspecting_throwsConflict() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer)
                .status(RefundStatus.RETURN_RECEIVED).build();
        CompleteInspectionRequestDTO request = new CompleteInspectionRequestDTO();
        request.setPassed(true);

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));

        assertThatThrownBy(() -> refundRequestService.completeInspection(9L, 200L, request))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- markRefundProcessed ----------

    @Test
    void markRefundProcessed_success() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.REFUND_PROCESSING).build();

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(refundRequestRepository.save(refundRequest)).thenReturn(refundRequest);

        refundRequestService.markRefundProcessed(9L, 200L);

        assertThat(refundRequest.getStatus()).isEqualTo(RefundStatus.REFUND_COMPLETED);
        assertThat(refundRequest.getRefundProcessedBy()).isEqualTo(admin);
    }

    @Test
    void markRefundProcessed_whenNotRefundProcessing_throwsConflict() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.INSPECTING).build();

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));

        assertThatThrownBy(() -> refundRequestService.markRefundProcessed(9L, 200L))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- closeRequest ----------

    @Test
    void closeRequest_fromRefundCompleted_success() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.REFUND_COMPLETED).build();

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(refundRequestRepository.save(refundRequest)).thenReturn(refundRequest);

        refundRequestService.closeRequest(9L, 200L);

        assertThat(refundRequest.getStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(refundRequest.getCompletedBy()).isEqualTo(admin);
    }

    @Test
    void closeRequest_whenInvalidStatus_throwsConflict() {
        RefundRequest refundRequest = RefundRequest.builder().id(200L).order(order).requestedBy(customer).status(RefundStatus.UNDER_REVIEW).build();

        when(refundRequestRepository.findById(200L)).thenReturn(Optional.of(refundRequest));

        assertThatThrownBy(() -> refundRequestService.closeRequest(9L, 200L))
                .isInstanceOf(ConflictException.class);

        verify(refundRequestRepository, never()).save(any());
    }
}
