package com.praneeth.identityservice.mapper;

import com.praneeth.identityservice.dto.CompleteRegistrationRequest;
import com.praneeth.identityservice.dto.UserResponse;
import com.praneeth.identityservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(CompleteRegistrationRequest request);
}
