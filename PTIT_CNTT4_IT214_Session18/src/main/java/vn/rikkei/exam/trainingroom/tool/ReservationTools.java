package vn.rikkei.exam.trainingroom.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vn.rikkei.exam.trainingroom.exception.ConflictException;
import vn.rikkei.exam.trainingroom.exception.ResourceNotFoundException;
import vn.rikkei.exam.trainingroom.model.*;
import vn.rikkei.exam.trainingroom.repository.AppUserRepository;
import vn.rikkei.exam.trainingroom.repository.ReservationRequestRepository;
import vn.rikkei.exam.trainingroom.repository.ResourceInventoryRepository;
import vn.rikkei.exam.trainingroom.repository.ResourceTypeRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationTools {

    private final ResourceTypeRepository resourceTypeRepository;
    private final ResourceInventoryRepository resourceInventoryRepository;
    private final AppUserRepository appUserRepository;
    private final ReservationRequestRepository reservationRequestRepository;

    private final List<String> usedTools = new ArrayList<>();

    public void resetUsedTools() {
        usedTools.clear();
    }

    public List<String> getUsedTools() {
        return List.copyOf(usedTools);
    }

    @Tool(description = "Tra cứu số phòng còn trống theo mã loại phòng và khoảng ngày startDate/endDate. " +
            "Trả về mã loại phòng, khoảng ngày, số phòng còn lại thấp nhất trong khoảng đó, và trạng thái còn/hết phòng.")
    public String getRoomAvailability(
            @ToolParam(description = "Mã loại phòng, ví dụ STD, PRM") String resourceCode,
            @ToolParam(description = "Ngày bắt đầu(startDate) định dạng yyyy-MM-dd") String startDate,
            @ToolParam(description = "Ngày kết thúc(endDate) định dạng yyyy-MM-dd") String endDate
    ) {
        usedTools.add("getRoomAvailability");
        log.info("Tool gọi: getRoomAvailability({}, {}, {})", resourceCode, startDate, endDate);

        if (resourceCode == null || resourceCode.isBlank() || startDate == null || startDate.isBlank()
                || endDate == null || endDate.isBlank()) {
            throw new ConflictException("Thiếu thông tin bắt buộc: resourceCode, startDate hoặc endDate");
        }

        ResourceType resourceTypeEntity = resourceTypeRepository.findById(resourceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng: " + resourceCode));

        LocalDate checkInDate;
        LocalDate checkOutDate;
        try {
            checkInDate = LocalDate.parse(startDate);
            checkOutDate = LocalDate.parse(endDate);
        } catch (DateTimeParseException e) {
            throw new ConflictException("Ngày không đúng định dạng yyyy-MM-dd");
        }

        if (!checkInDate.isBefore(checkOutDate)) {
            throw new ConflictException("startDate phải trước endDate");
        }

        List<LocalDate> dateRange = new ArrayList<>();
        for (LocalDate date = checkInDate; date.isBefore(checkOutDate); date = date.plusDays(1)) {
            dateRange.add(date);
        }

        int minAvailable = dateRange.stream()
                .mapToInt(date -> resourceInventoryRepository
                        .findByResourceType_ResourceCodeAndAvailableDate(resourceCode, date)
                        .map(ResourceInventory::getAvailableSlots)
                        .orElse(0))
                .min()
                .orElse(0);

        boolean available = minAvailable > 0;

        return String.format(
                "Loại phòng: %s (%s). Từ %s đến %s. Số phòng còn lại thấp nhất: %d. Trạng thái: %s.",
                resourceTypeEntity.getDisplayName(), resourceCode, checkInDate, checkOutDate,
                minAvailable, available ? "còn phòng" : "hết phòng"
        );
    }

    @Tool(description = "Tạo yêu cầu đặt phòng ở trạng thái PENDING chờ phê duyệt. " +
            "Kiểm tra người dùng tồn tại, tối đa 14 ngày, sức chứa phòng, Premium tối thiểu 2 khách, và purpose 10-200 ký tự.")
    public String createReservationRequest(
            @ToolParam(description = "Mã người dùng, ví dụ USR-001") String userId,
            @ToolParam(description = "Mã loại phòng, ví dụ STD, PRM") String resourceType,
            @ToolParam(description = "Ngày bắt đầu(startDate) định dạng yyyy-MM-dd") String startDate,
            @ToolParam(description = "Ngày kết thúc(endDate) định dạng yyyy-MM-dd") String endDate,
            @ToolParam(description = "Số lượng khách") Integer participantCount,
            @ToolParam(description = "Mục đích đặt phòng, 10-200 ký tự") String purpose
    ) {
        usedTools.add("createReservationRequest");
        log.info("Tool gọi: createReservationRequest({}, {}, {}, {}, {}, {})",
                userId, resourceType, startDate, endDate, participantCount, purpose);

        if (userId == null || userId.isBlank() || resourceType == null || resourceType.isBlank()
                || startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()
                || participantCount == null || purpose == null || purpose.isBlank()) {
            throw new ConflictException("Thiếu thông tin bắt buộc để tạo yêu cầu đặt phòng");
        }

        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + userId));

        ResourceType resourceTypeEntity = resourceTypeRepository.findById(resourceType)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng: " + resourceType));

        LocalDate checkInDate;
        LocalDate checkOutDate;
        try {
            checkInDate = LocalDate.parse(startDate);
            checkOutDate = LocalDate.parse(endDate);
        } catch (DateTimeParseException e) {
            throw new ConflictException("Ngày không đúng định dạng yyyy-MM-dd");
        }

        if (!checkInDate.isBefore(checkOutDate)) {
            throw new ConflictException("startDate phải trước endDate");
        }

        long days = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (days > 14) {
            throw new ConflictException("Số ngày tối đa cho phép là 14");
        }

        if (participantCount < 1) {
            throw new ConflictException("Số khách không hợp lệ");
        }

        if (participantCount > resourceTypeEntity.getMaxParticipants()) {
            throw new ConflictException(
                    "Số khách vượt quá sức chứa tối đa (" + resourceTypeEntity.getMaxParticipants() + ") của loại phòng " + resourceType);
        }

        if ("PRM".equals(resourceType) && participantCount < 2) {
            throw new ConflictException("Phòng Premium yêu cầu tối thiểu 2 khách");
        }

        if (purpose.length() < 10 || purpose.length() > 200) {
            throw new ConflictException("Mục đích đặt phòng phải từ 10 đến 200 ký tự");
        }

        String requestId = "RR-" + System.currentTimeMillis();

        ReservationRequest reservationRequest = ReservationRequest.builder()
                .requestId(requestId)
                .requester(appUser)
                .resourceType(resourceTypeEntity)
                .startDate(checkInDate)
                .endDate(checkOutDate)
                .participantCount(participantCount)
                .purpose(purpose)
                .status(ReservationStatus.PENDING)
                .build();

        reservationRequestRepository.save(reservationRequest);

        return String.format(
                "Đã tạo yêu cầu đặt phòng %s: %s cho người dùng %s, từ %s đến %s, %d khách. Trạng thái: PENDING.",
                requestId, resourceTypeEntity.getDisplayName(), appUser.getFullName(),
                checkInDate, checkOutDate, participantCount
        );
    }
}