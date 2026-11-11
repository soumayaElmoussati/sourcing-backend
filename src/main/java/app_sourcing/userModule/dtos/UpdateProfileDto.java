package app_sourcing.userModule.dtos;

import jakarta.persistence.*;
import lombok.Data;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
public class UpdateProfileDto{

    private Long id;
    private String firstName;
    private String lastName;

}
