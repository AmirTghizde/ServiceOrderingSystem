package com.Maktab101.SpringProject.service.impl;

import com.Maktab101.SpringProject.dto.order.FinishOrderDto;
import com.Maktab101.SpringProject.dto.suggestion.SendSuggestionDto;
import com.Maktab101.SpringProject.dto.users.CardPaymentDto;
import com.Maktab101.SpringProject.model.Order;
import com.Maktab101.SpringProject.model.SubServices;
import com.Maktab101.SpringProject.model.Suggestion;
import com.Maktab101.SpringProject.model.Technician;
import com.Maktab101.SpringProject.model.enums.OrderStatus;
import com.Maktab101.SpringProject.service.*;
import com.Maktab101.SpringProject.utils.exceptions.CustomException;
import com.Maktab101.SpringProject.utils.exceptions.NotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class OrderSuggestionImpl implements OrderSuggestionService {
    private final OrderService orderService;
    private final SuggestionService suggestionService;
    private final SubServicesService subServicesService;
    private final TechnicianService technicianService;

    @Autowired
    public OrderSuggestionImpl(OrderService orderService, SuggestionService suggestionService,
                               SubServicesService subServicesService, TechnicianService technicianService) {
        this.orderService = orderService;
        this.suggestionService = suggestionService;
        this.subServicesService = subServicesService;
        this.technicianService = technicianService;
    }

    @Override
    @Transactional
    public void selectSuggestion(Long orderId, Long suggestionId) {
        log.info("Customer with order [{}] Selecting suggestion [{}]", orderId, suggestionId);
        Suggestion suggestion = suggestionService.findById(suggestionId);
        Order order = orderService.findById(orderId);

        if (!order.getSuggestions().contains(suggestion)) {
            log.error("Suggestion isn't for this order throwing exception");
            throw new NotFoundException("Couldn't find that suggestion in your orders");
        }
        switch (order.getOrderStatus()) {
            case AWAITING_TECHNICIAN_ARRIVAL, STARTED, FINISHED, PAID -> {
                log.error("Invalid order status throwing exception");
                throw new CustomException("You can't select suggestions anymore");
            }
        }

        order.setOrderStatus(OrderStatus.AWAITING_TECHNICIAN_ARRIVAL);
        order.setPrice(suggestion.getSuggestedPrice());
        order.setSelectedSuggestionId(suggestion.getId());
        orderService.save(order);
    }


    @Override
    @Transactional
    public void sendSuggestion(Long technicianId, SendSuggestionDto sendSuggestionDto) {
        log.info("Technician with id [{}] is trying to send a new suggestion [{}] for this order [{}]"
                , technicianId, sendSuggestionDto, sendSuggestionDto.getOrderID());

        Order order = orderService.findById(sendSuggestionDto.getOrderID());
        SubServices subServices = subServicesService.findById(order.getSubServices().getId());
        Technician technician = technicianService.findById(technicianId);

        checkCondition(technician, sendSuggestionDto, subServices, order);
        Suggestion suggestion = mapDtoValues(technician, sendSuggestionDto);

        order.getSuggestions().add(suggestion);
        order.setOrderStatus(OrderStatus.AWAITING_TECHNICIAN_SELECTION);
        suggestion.setOrder(order);

        try {
            log.info("Connecting to [{}]", suggestionService);
            orderService.save(order);
            suggestionService.save(suggestion);
        } catch (PersistenceException e) {
            log.error("PersistenceException occurred throwing CustomException ... ");
            throw new CustomException(e.getMessage());
        }
    }

    @Override
    public List<Suggestion> getSuggestionByTechnicianPoint(Long orderId, boolean ascending) {
        log.info("Getting suggestions ordered by technicianPoints for order [{}]", orderId);
        Order order = orderService.findById(orderId);
        List<Suggestion> suggestions = order.getSuggestions();
        Comparator<Suggestion> scoreComparing = Comparator.comparingDouble(s -> s.getTechnician().getScore());
        if (!ascending) {
            scoreComparing = scoreComparing.reversed();
        }
        suggestions.sort(scoreComparing);
        return suggestions;
    }

    @Override
    @Transactional
    public List<Suggestion> getSuggestionByPrice(Long orderId, boolean ascending) {
        log.info("Getting suggestions ordered by price for order [{}]", orderId);
        Order order = orderService.findById(orderId);
        List<Suggestion> suggestions = order.getSuggestions();
        Comparator<Suggestion> priceComparing = Comparator.comparing(Suggestion::getSuggestedPrice);
        if (!ascending) {
            priceComparing = priceComparing.reversed();
        }
        suggestions.sort(priceComparing);
        return suggestions;
    }

    @Override
    public long isAfterSuggestedTime(Long orderId) {
        log.info("Calculating order time [orderId:{}]", orderId);
        Order order = orderService.findById(orderId);
        Suggestion suggestion = suggestionService.findById(order.getSelectedSuggestionId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime suggestedDateTime = suggestion.getSuggestedDate();
        LocalTime suggestedDuration = suggestion.getDuration();
        Duration duration = Duration.between(LocalTime.MIN, suggestedDuration);
        LocalDateTime endDateTime = suggestedDateTime.plus(duration);

        if (now.isBefore(endDateTime)) {
            return 0;
        } else {
            long difference = ChronoUnit.HOURS.between(now, endDateTime);
            difference = Math.abs(difference);
            log.info("Hour difference is [{}]", difference);
            return difference;
        }
    }

    @Override
    @Transactional
    public void handelFinishOrder(FinishOrderDto dto) {
        log.info("Order[id:{}] finished, handling the aftermath", dto.getId());
        // Finish the order
        orderService.finishOrder(dto.getId(), dto.getPoint());
        if (dto.getComment() != null) {
            orderService.addComment(dto.getId(), dto.getComment());
        }

        // Add the technician points
        Order order = orderService.findById(dto.getId());
        Suggestion suggestion = suggestionService.findById(order.getSelectedSuggestionId());
        Technician technician = suggestion.getTechnician();
        technicianService.addPoints(technician.getId(), dto.getPoint());


        // If the finished time is after now returns more than 0(Returns the hour difference)
        long hourDifference = isAfterSuggestedTime(dto.getId());
        if (hourDifference > 0) {
            technicianService.reducePoints(technician.getId(), hourDifference);
        }
    }

    @Transactional
    public void payOnline(CardPaymentDto dto, int captcha) {
        Order order = orderService.findById(dto.getOrderId());
        checkOnlinePaymentConditions(order, dto, captcha);

        Suggestion suggestion = suggestionService.findById(order.getSelectedSuggestionId());
        Long technicianId = suggestion.getTechnician().getId();

        technicianService.addCredit(technicianId, dto.getAmount());
        order.setOrderStatus(OrderStatus.PAID);
        orderService.save(order);
    }

    private void checkOnlinePaymentConditions(Order order, CardPaymentDto dto, int captcha) {
        String actualCaptcha = String.valueOf(captcha);

        if (order.getSelectedSuggestionId() == null) {
            throw new NotFoundException("Can't find the technician of this order");
        } else if (!order.getOrderStatus().equals(OrderStatus.FINISHED)) {
            throw new CustomException("You can't pay in now");
        }
        if (!dto.getCaptcha().equals(actualCaptcha)) {
            throw new CustomException("Captcha don't match");
        }
    }

    protected void checkCondition(Technician technician, SendSuggestionDto sendSuggestionDto, SubServices subServices, Order order) {
        log.info("Checking suggestion conditions");
        switch (order.getOrderStatus()) {
            case AWAITING_TECHNICIAN_ARRIVAL, STARTED, FINISHED, PAID -> {
                log.error("Invalid order status throwing exception");
                throw new CustomException("You can't send a suggestion for this order");
            }
        }
        List<Technician> technicians = subServices.getTechnicians();
        if (!technicians.contains(technician)) {
            log.error("Technician doesn't have this service throwing exception");
            throw new CustomException("You don't have this sub service");
        }
        if (sendSuggestionDto.getSuggestedPrice() < subServices.getBaseWage()) {
            log.error("SuggestedPrice is lower than base wage throwing exception");
            throw new CustomException("Price can't be lower than base wage");
        }
        LocalDateTime localDateTime = toLocalDateTime(sendSuggestionDto.getSuggestedTime(), sendSuggestionDto.getSuggestedDate());
        LocalDateTime now = LocalDateTime.now();
        if (localDateTime.isBefore(now)) {
            log.error("Date is before now throwing exception");
            throw new CustomException("Date and time can't be before now");
        }
    }

    protected LocalDateTime toLocalDateTime(String time, String date) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        LocalDate localDate = LocalDate.parse(date, dateFormatter);
        LocalTime localTime = LocalTime.parse(time, timeFormatter);

        return localDate.atTime(localTime);
    }

    protected LocalTime convertTime(String duration) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return LocalTime.parse(duration, timeFormatter);
    }

    protected Suggestion mapDtoValues(Technician technician, SendSuggestionDto sendSuggestionDto) {
        log.info("Mapping Dto values [{}]", sendSuggestionDto);
        Suggestion suggestion = new Suggestion();
        suggestion.setDate(LocalDateTime.now());
        suggestion.setSuggestedPrice(sendSuggestionDto.getSuggestedPrice());
        suggestion.setTechnician(technician);

        LocalDateTime localDateTime = toLocalDateTime(sendSuggestionDto.getSuggestedTime(), sendSuggestionDto.getSuggestedDate());
        suggestion.setSuggestedDate(localDateTime);

        suggestion.setDuration(convertTime(sendSuggestionDto.getDuration()));
        return suggestion;
    }
}
