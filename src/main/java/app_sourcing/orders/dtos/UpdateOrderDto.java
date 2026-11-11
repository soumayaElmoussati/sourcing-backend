package app_sourcing.orders.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateOrderDto {

    @NotBlank
    private String customerName;
    @NotBlank
    private String customerPhone;

    @NotBlank
    private String city;
    @NotBlank
    private String fullAddress;

    private String note;
}
