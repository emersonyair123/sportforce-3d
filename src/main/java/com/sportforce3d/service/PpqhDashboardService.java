package com.sportforce3d.service;

import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhPaymentStatus;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.repository.HepyDashboardRepository;
import com.sportforce3d.repository.HepyDashboardRepository.HepyTopProduct;
import com.sportforce3d.repository.HepyDashboardRepository.HepyTotal;
import com.sportforce3d.repository.HepyOrderRepository;
import com.sportforce3d.repository.HepyPaymentRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public class PpqhDashboardService {

    public static final ZoneId PPQH_LIMA = ZoneId.of("America/Lima");
    public static final int PPQH_DAYS = 30;

    public record PpqhStats(LocalDate ppqhToday, HepyTotal ppqhTodaySales, HepyTotal ppqhMonthSales, HepyTotal ppqhPreviousMonthSales,
                        int ppqhPendingPayments, Map<PpqhOrderStatus, Integer> ppqhOrdersByStatus,
                        Map<LocalDate, Long> ppqhDaily, Map<LocalDate, Long> ppqhPrevious, List<HepyTopProduct> ppqhTopProducts) {
    }

    private final HepyDashboardRepository ppqhDashboard;
    private final HepyOrderRepository ppqhOrders;
    private final HepyPaymentRepository ppqhPayments;
    private final PpqhSession ppqhSession;
    private final Clock ppqhClock;

    public PpqhDashboardService(HepyDashboardRepository ppqhDashboard, HepyOrderRepository ppqhOrders, HepyPaymentRepository ppqhPayments,
                            PpqhSession ppqhSession, Clock ppqhClock) {
        this.ppqhDashboard = ppqhDashboard;
        this.ppqhOrders = ppqhOrders;
        this.ppqhPayments = ppqhPayments;
        this.ppqhSession = ppqhSession;
        this.ppqhClock = ppqhClock;
    }

    public PpqhStats ppqhStats() {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        LocalDate ppqhToday = LocalDate.now(ppqhClock.withZone(PPQH_LIMA));
        LocalDate ppqhMonthStart = ppqhToday.withDayOfMonth(1);
        LocalDate ppqhPrevStart = ppqhMonthStart.minusMonths(1);
        LocalDate ppqhPrevEnd = ppqhPrevStart.plusDays(Math.min(ppqhToday.getDayOfMonth(), ppqhPrevStart.lengthOfMonth()) - 1L);
        LocalDate ppqhFrom = ppqhToday.minusDays(PPQH_DAYS - 1L);
        return new PpqhStats(ppqhToday, ppqhDashboard.hepySalesBetween(ppqhToday, ppqhToday), ppqhDashboard.hepySalesBetween(ppqhMonthStart, ppqhToday),
                ppqhDashboard.hepySalesBetween(ppqhPrevStart, ppqhPrevEnd), ppqhPayments.hepyCountByStatus(PpqhPaymentStatus.EN_VERIFICACION),
                ppqhOrders.hepyCountByStatus(), ppqhDashboard.hepyDailySales(ppqhFrom, ppqhToday),
                ppqhDashboard.hepyDailySales(ppqhFrom.minusDays(PPQH_DAYS), ppqhFrom.minusDays(1)), ppqhDashboard.hepyTopProducts(5));
    }
}
