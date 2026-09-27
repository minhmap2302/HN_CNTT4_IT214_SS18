package vn.rikkei.exam.trainingroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.rikkei.exam.trainingroom.dto.ApprovalRequest;
import vn.rikkei.exam.trainingroom.dto.ApprovalResponse;
import vn.rikkei.exam.trainingroom.exception.ConflictException;
import vn.rikkei.exam.trainingroom.exception.ResourceNotFoundException;
import vn.rikkei.exam.trainingroom.model.ReservationRequest;
import vn.rikkei.exam.trainingroom.model.ReservationStatus;
import vn.rikkei.exam.trainingroom.model.ResourceType;
import vn.rikkei.exam.trainingroom.repository.ReservationRequestRepository;

import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRequestRepository reservationRequestRepository;

    public ApprovalResponse decide(ApprovalRequest request) {
        ReservationRequest reservation = reservationRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu đặt phòng: " + request.getRequestId()));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ConflictException(
                    "Chỉ xử lý được yêu cầu ở trạng thái PENDING, hiện tại: " + reservation.getStatus());
        }

        String decision = request.getDecision();
        if (!"APPROVE".equals(decision) && !"REJECT".equals(decision)) {
            throw new ConflictException("decision chỉ nhận APPROVE hoặc REJECT");
        }

        if ("APPROVE".equals(decision)) {
            revalidate(reservation);
        }

        reservation.setStatus("APPROVE".equals(decision) ? ReservationStatus.APPROVED : ReservationStatus.REJECTED);
        reservation.setDecisionNote(request.getNote());
        reservationRequestRepository.save(reservation);

        log.info("Reservation {} được xử lý: {}", reservation.getRequestId(), reservation.getStatus());

        return ApprovalResponse.builder()
                .requestId(reservation.getRequestId())
                .status(reservation.getStatus().name())
                .decisionNote(reservation.getDecisionNote())
                .build();
    }

    private void revalidate(ReservationRequest reservation) {
        long days = ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate());
        if (days > 14) {
            throw new ConflictException("Số ngày vượt quá giới hạn cho phép (14 ngày)");
        }

        ResourceType resourceType = reservation.getResourceType();
        if (reservation.getParticipantCount() > resourceType.getMaxParticipants()) {
            throw new ConflictException(
                    "Số khách vượt quá sức chứa tối đa (" + resourceType.getMaxParticipants() + ") của loại phòng " + resourceType.getResourceCode());
        }

        if ("PRM".equals(resourceType.getResourceCode()) && reservation.getParticipantCount() < 2) {
            throw new ConflictException("Phòng Premium yêu cầu tối thiểu 2 khách");
        }
    }
}
