package com.primeraPulpa.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.primeraPulpa.dto.MateriaPrimaDTO;
import com.primeraPulpa.entities.MateriaPrima;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
        // uses = {PersonaMapper.class}
)
public interface MateriaPrimaMapper extends BaseMapper<MateriaPrima, MateriaPrimaDTO, Long> {

    @Override
    @Mapping(target = "unidadMedidaId", source = "unidadMedida.id")
    MateriaPrimaDTO toDTO(MateriaPrima entity);

}
