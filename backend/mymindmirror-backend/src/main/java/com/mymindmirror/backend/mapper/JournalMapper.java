package com.mymindmirror.backend.mapper;

import com.mymindmirror.backend.model.JournalEntry;
import com.mymindmirror.backend.model.KeyPhrase;
import com.mymindmirror.backend.payload.response.JournalEntryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {JsonMapperHelper.class, UserMapper.class})
public interface JournalMapper {

    @Mapping(target = "emotions", source = "emotions", qualifiedByName = "jsonToMap")
    @Mapping(target = "coreConcerns", source = "coreConcerns", qualifiedByName = "jsonToStringList")
    @Mapping(target = "growthTips", source = "growthTips", qualifiedByName = "jsonToStringList")
    @Mapping(target = "keyPhrases", source = "keyPhrases", qualifiedByName = "mapKeyPhrases")
    JournalEntryResponse toResponse(JournalEntry entry);

    @Named("mapKeyPhrases")
    default List<String> mapKeyPhrases(List<KeyPhrase> keyPhrases) {
        if (keyPhrases == null) return Collections.emptyList();
        return keyPhrases.stream().map(KeyPhrase::getPhrase).collect(Collectors.toList());
    }
}