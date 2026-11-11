package app_sourcing.userModule.service;


import app_sourcing.userModule.dtos.UpdateProfileDto;
import app_sourcing.userModule.dtos.UserDto;
import app_sourcing.userModule.entitie.User;
import app_sourcing.userModule.mappers.UserMapper;
import app_sourcing.userModule.repo.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    public UserDto getProfile(Long userID){
        User user=userRepository.findById(userID).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return UserMapper.toDto(user);
    }

    public UserDto updateProfile(UpdateProfileDto profile){
        User user=userRepository.findById(profile.getId()).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setLastName(profile.getLastName());
        user.setFirstName(profile.getFirstName());
        return  UserMapper.toDto(userRepository.save(user));
    }


}
