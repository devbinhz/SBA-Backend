package com.bookverse.service.statistics.impl;

import com.bookverse.config.MinioProperties;
import com.bookverse.dto.response.statistics.BookSellingStatsDTO;
import com.bookverse.dto.response.statistics.StatisticsOverviewResponseDTO;
import com.bookverse.enums.OrderStatus;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.OrderRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.statistics.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final MinioProperties minioProperties;

    @Override
    @Transactional(readOnly = true)
    public StatisticsOverviewResponseDTO getOverview() {
        long totalUsers = userRepository.count();
        long totalBooks = bookRepository.count();
        long activeBooks = bookRepository.countByActiveTrue();
        long totalOrders = orderRepository.count();

        List<OrderStatus> revenueStatuses = List.of(
                OrderStatus.PAID,
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED
        );
        long recognizedRevenue = orderRepository.sumRevenueByStatuses(revenueStatuses);

        List<BookSellingStatsDTO> topSellingBooks = bookRepository.findTopSellingBooks(PageRequest.of(0, 10))
                .stream()
                .map(book -> {
                    String coverUrl = book.getCoverUrl();
                    if ((coverUrl == null || coverUrl.isBlank()) && book.getCoverKey() != null && !book.getCoverKey().isBlank()) {
                        coverUrl = minioProperties.endpoint() + "/" + minioProperties.thumbnailsBucket() + "/" + book.getCoverKey();
                    }
                    return BookSellingStatsDTO.builder()
                            .id(book.getId())
                            .title(book.getTitle())
                            .author(book.getAuthor())
                            .coverUrl(coverUrl)
                            .soldCount(book.getSoldCount())
                            .build();
                })
                .toList();

        return StatisticsOverviewResponseDTO.builder()
                .totalUsers(totalUsers)
                .totalBooks(totalBooks)
                .activeBooks(activeBooks)
                .totalOrders(totalOrders)
                .recognizedRevenue(recognizedRevenue)
                .topSellingBooks(topSellingBooks)
                .build();
    }
}
