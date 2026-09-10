package com.zamazor.market.modules.billing.models.mapper;

import com.stripe.model.checkout.Session;
import com.zamazor.market.modules.billing.models.dto.PaymentSessionResponse;
import com.zamazor.market.shared.util.DateTimeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public interface PaymentMapper {
	@Mapping(target = "paymentUrl", source = "session.url")
	@Mapping(target = "sessionId", source = "session.id")
	@Mapping(target = "expiresAt", source = "session.expiresAt")
	PaymentSessionResponse toDto(Session session);
}
