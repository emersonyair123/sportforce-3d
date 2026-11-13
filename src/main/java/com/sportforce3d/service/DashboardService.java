package com.sportforce3d.service;

import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.PaymentStatus;
import com.sportforce3d.model.Role;
import com.sportforce3d.repository.DashboardRepository;
import com.sportforce3d.repository.DashboardRepository.TopProduct;
import com.sportforce3d.repository.DashboardRepository.Total;
import com.sportforce3d.repository.OrderRepository;
import com.sportforce3d.repository.PaymentRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public class DashboardService {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");
    public static final int DAYS = 30;

    public record Stats(LocalDate today, Total todaySales, Total monthSales, Total previousMonthSales,
                        int pendingPayments, Map<OrderStatus, Integer> ordersByStatus,
                        Map<LocalDate, Long> daily, Map<LocalDate, Long> previous, List<TopProduct> topProducts) {
    }

    private final DashboardRepository dashboard;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final Session session;
    private final Clock clock;

    public DashboardService(DashboardRepository dashboard, OrderRepository orders, PaymentRepository payments,
                            Session session, Clock clock) {
        this.dashboard = dashboard;
        this.orders = orders;
        this.payments = payments;
        this.session = session;
        this.clock = clock;
    }

    public Stats stats() {
        session.requireRole(Role.ADMIN);
        LocalDate today = LocalDate.now(clock.withZone(LIMA));
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate prevStart = monthStart.minusMonths(1);
        LocalDate prevEnd = prevStart.plusDays(Math.min(today.getDayOfMonth(), prevStart.lengthOfMonth()) - 1L);
        LocalDate from = today.minusDays(DAYS - 1L);
        return new Stats(today, dashboard.salesBetween(today, today), dashboard.salesBetween(monthStart, today),
                dashboard.salesBetween(prevStart, prevEnd), payments.countByStatus(PaymentStatus.EN_VERIFICACION),
                orders.countByStatus(), dashboard.dailySales(from, today),
                dashboard.dailySales(from.minusDays(DAYS), from.minusDays(1)), dashboard.topProducts(5));
    }
}
