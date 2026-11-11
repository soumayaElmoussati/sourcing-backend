package app_sourcing.orders.mappers;

import app_sourcing.orders.dtos.OrderResponseDto;
import app_sourcing.orders.entities.Order;

import java.util.List;
import java.util.stream.Collectors;

public class OrderMapper {

    private OrderMapper() {}

    public static OrderResponseDto toDto(Order order) {

        OrderResponseDto dto = new OrderResponseDto();

        dto.setId(order.getId());

        // references
        dto.setSellerId(order.getSeller().getId());
        dto.setProductId(order.getProduct().getId());

        // customer
        dto.setCustomerName(order.getCustomer().getCustomerName());
        dto.setCustomerPhone(order.getCustomer().getCustomerPhone());

        // address
        dto.setCountry(order.getCountry());
        dto.setCity(order.getCustomer().getCity());
        dto.setFullAddress(order.getCustomer().getFullAddress());
        dto.setNote(order.getNote());

        // order data
        dto.setQuantity(order.getQuantity());
        dto.setUnitPrice(order.getUnitPrice());
        dto.setTotalPrice(order.getTotalPrice());

        dto.setPaymentMode(order.getPaymentMode());
        dto.setStatus(order.getStatus());

        // inherited
        dto.setFulfillmentMode(order.getFulfillmentMode());
        dto.setShippingType(order.getShippingType());
        dto.setVatAmount(order.getVatAmount());

        dto.setCreatedAt(order.getCreatedAt());

        dto.setSource(order.getSource());
        dto.setExternalOrderId(order.getExternalOrderId());

        return dto;
    }

    public static List<OrderResponseDto> toDtoList(List<Order> orders) {
        return orders.stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }
}
