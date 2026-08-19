package com.roletadefilmes.social.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roletadefilmes.roulette.api.dto.RouletteMovieResponse;
import com.roletadefilmes.roulette.api.dto.RouletteSpinRequest;
import com.roletadefilmes.roulette.service.RouletteService;
import com.roletadefilmes.social.api.dto.SocialProviderResponse;
import com.roletadefilmes.social.api.dto.SocialRoomMemberResponse;
import com.roletadefilmes.social.api.dto.SocialRoomResponse;
import com.roletadefilmes.social.api.dto.SocialRoomSummaryResponse;
import com.roletadefilmes.social.api.dto.SocialSpinResponse;
import com.roletadefilmes.social.api.dto.UpdateSocialPreferenceRequest;
import com.roletadefilmes.social.domain.SocialRoomMemberRole;
import com.roletadefilmes.social.domain.SocialRoomStatus;
import com.roletadefilmes.social.domain.SocialRoomType;
import com.roletadefilmes.social.domain.exception.InvalidSocialRoomActionException;
import com.roletadefilmes.social.domain.exception.SocialRoomAccessDeniedException;
import com.roletadefilmes.social.domain.exception.SocialRoomConflictException;
import com.roletadefilmes.social.domain.exception.SocialRoomNotFoundException;
import com.roletadefilmes.social.persistence.entity.SocialRoomEntity;
import com.roletadefilmes.social.persistence.entity.SocialRoomMemberEntity;
import com.roletadefilmes.social.persistence.entity.SocialRoomSpinEntity;
import com.roletadefilmes.social.persistence.repository.SocialRoomMemberRepository;
import com.roletadefilmes.social.persistence.repository.SocialRoomRepository;
import com.roletadefilmes.social.persistence.repository.SocialRoomSpinRepository;
import com.roletadefilmes.streaming.persistence.entity.StreamingProviderEntity;
import com.roletadefilmes.streaming.persistence.repository.UserStreamingPreferenceRepository;
import com.roletadefilmes.user.domain.exception.UserNotFoundException;
import com.roletadefilmes.user.persistence.repository.UserAccountRepository;
import com.roletadefilmes.vibe.persistence.repository.VibeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SocialRoomService {

    private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_LENGTH = 8;
    private static final int MAX_INVITE_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<>() { };

    private final SocialRoomRepository roomRepository;
    private final SocialRoomMemberRepository memberRepository;
    private final SocialRoomSpinRepository roomSpinRepository;
    private final UserAccountRepository userRepository;
    private final UserStreamingPreferenceRepository preferenceRepository;
    private final RouletteService rouletteService;
    private final VibeRepository vibeRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SocialRoomService(
            SocialRoomRepository roomRepository,
            SocialRoomMemberRepository memberRepository,
            SocialRoomSpinRepository roomSpinRepository,
            UserAccountRepository userRepository,
            UserStreamingPreferenceRepository preferenceRepository,
            RouletteService rouletteService,
            VibeRepository vibeRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.roomSpinRepository = roomSpinRepository;
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.rouletteService = rouletteService;
        this.vibeRepository = vibeRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public SocialRoomResponse create(UUID userId, SocialRoomType type) {
        var owner = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var room = roomRepository.saveAndFlush(
                new SocialRoomEntity(owner, type, generateInviteCode())
        );
        memberRepository.saveAndFlush(
                new SocialRoomMemberEntity(room, owner, SocialRoomMemberRole.HOST)
        );
        return toResponse(room, userId, null);
    }

    @Transactional
    public SocialRoomResponse join(UUID userId, String rawInviteCode) {
        var inviteCode = rawInviteCode.trim().toUpperCase(Locale.ROOT);
        var room = roomRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(SocialRoomNotFoundException::new);
        ensureOpen(room);

        if (memberRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
            return toResponse(room, userId, null);
        }
        if (memberRepository.countByRoomId(room.getId()) >= room.getRoomType().capacity()) {
            throw new SocialRoomConflictException("Esta sala já atingiu o limite de participantes.");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        memberRepository.saveAndFlush(
                new SocialRoomMemberEntity(room, user, SocialRoomMemberRole.MEMBER)
        );
        return toResponse(room, userId, null);
    }

    @Transactional(readOnly = true)
    public SocialRoomResponse get(UUID userId, UUID roomId) {
        var room = roomRepository.findById(roomId)
                .orElseThrow(SocialRoomNotFoundException::new);
        ensureMembership(roomId, userId);
        return toResponse(room, userId, null);
    }

    @Transactional(readOnly = true)
    public List<SocialRoomSummaryResponse> list(UUID userId) {
        return roomRepository.findAllForUser(userId).stream()
                .map(room -> new SocialRoomSummaryResponse(
                        room.getId(),
                        room.getRoomType(),
                        room.getStatus(),
                        room.getOwner().getId().equals(userId),
                        Math.toIntExact(memberRepository.countByRoomId(room.getId())),
                        room.getRoomType().capacity(),
                        room.getSpinSequence(),
                        room.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional
    public void leave(UUID userId, UUID roomId) {
        var room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(SocialRoomNotFoundException::new);
        ensureMembership(roomId, userId);

        if (room.getOwner().getId().equals(userId)) {
            if (room.isOpen()) {
                room.close(Instant.now(clock));
                roomRepository.save(room);
            }
            return;
        }
        memberRepository.deleteMembership(roomId, userId);
    }

    @Transactional
    public SocialRoomResponse updatePreference(
            UUID userId,
            UUID roomId,
            UpdateSocialPreferenceRequest request
    ) {
        var room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(SocialRoomNotFoundException::new);
        ensureOpen(room);
        var member = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new SocialRoomAccessDeniedException(
                        "Você não participa desta sala."
                ));

        var genreIds = request.genreIds().stream().sorted().toArray(Integer[]::new);
        var vibe = request.vibeId() == null
                ? null
                : vibeRepository.findById(request.vibeId())
                        .filter(candidate -> candidate.isActive())
                        .orElseThrow(() -> new InvalidSocialRoomActionException(
                                "A vibe selecionada não está disponível."
                        ));
        if (request.ready() && genreIds.length == 0 && vibe == null) {
            throw new InvalidSocialRoomActionException(
                    "Escolha pelo menos um gênero ou uma vibe antes de confirmar."
            );
        }

        member.updatePreferences(genreIds, vibe, request.ready(), Instant.now(clock));
        memberRepository.saveAndFlush(member);
        return toResponse(room, userId, null);
    }

    @Transactional
    public SocialSpinResponse spin(
            UUID userId,
            UUID roomId,
            RouletteSpinRequest request
    ) {
        var room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(SocialRoomNotFoundException::new);
        ensureOpen(room);
        ensureHost(room, userId);

        var members = memberRepository.findAllWithUserByRoomId(roomId);
        if (members.size() < 2) {
            throw new InvalidSocialRoomActionException(
                    "Convide pelo menos uma pessoa antes de girar."
            );
        }

        var existingSpin = roomSpinRepository.findByRoomIdAndIdempotencyKey(
                roomId,
                request.idempotencyKey().toString()
        );
        if (existingSpin.isPresent()) {
            var replayedResponse = rouletteService.spinForRoom(userId, roomId, request);
            return new SocialSpinResponse(
                    toResponse(room, userId, existingSpin.orElseThrow()),
                    replayedResponse.movie(),
                    replayedResponse.quota()
            );
        }

        if (members.stream().anyMatch(member -> !member.isReady())) {
            throw new InvalidSocialRoomActionException(
                    "Todos os participantes precisam confirmar seus palpites antes do giro."
            );
        }
        if (request.genreId() != null || request.vibeId() != null) {
            throw new InvalidSocialRoomActionException(
                    "No modo social, gênero e vibe são definidos pelos palpites dos participantes."
            );
        }

        var commonProviderIds = commonProviders(members).stream()
                .map(provider -> provider.entity().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (commonProviderIds.isEmpty()) {
            throw new InvalidSocialRoomActionException(
                    "Os participantes ainda não possuem um streaming em comum."
            );
        }
        if (!commonProviderIds.containsAll(request.providerIds())) {
            throw new InvalidSocialRoomActionException(
                    "Selecione apenas streamings disponíveis para todos os participantes."
            );
        }

        var rouletteResponse = rouletteService.spinForRoom(userId, roomId, request);

        var spinNumber = room.nextSpinNumber();
        var socialSpin = roomSpinRepository.saveAndFlush(new SocialRoomSpinEntity(
                room,
                room.getOwner(),
                request.idempotencyKey().toString(),
                spinNumber,
                buildFilters(request, members),
                objectMapper.convertValue(rouletteResponse.movie(), MAP_TYPE)
        ));
        roomSpinRepository.snapshotParticipants(socialSpin.getId(), roomId, members.size());
        members.forEach(SocialRoomMemberEntity::resetReady);
        memberRepository.saveAll(members);
        roomRepository.saveAndFlush(room);

        return new SocialSpinResponse(
                toResponse(room, userId, socialSpin),
                rouletteResponse.movie(),
                rouletteResponse.quota()
        );
    }

    private SocialRoomResponse toResponse(
            SocialRoomEntity room,
            UUID currentUserId,
            SocialRoomSpinEntity knownLastSpin
    ) {
        var members = memberRepository.findAllWithUserByRoomId(room.getId());
        var preferencesByUser = preferencesByUser(members);
        var commonProviders = commonProviders(members, preferencesByUser).stream()
                .map(ProviderChoice::response)
                .toList();
        var memberResponses = members.stream()
                .map(member -> new SocialRoomMemberResponse(
                        member.getUser().getId(),
                        member.getUser().getDisplayName(),
                        member.getRole() == SocialRoomMemberRole.HOST,
                        member.getJoinedAt(),
                        preferencesByUser.getOrDefault(member.getUser().getId(), List.of()).stream()
                                .map(this::toProviderResponse)
                                .toList(),
                        Arrays.stream(member.getSelectedGenreIds()).sorted().toList(),
                        member.getSelectedVibe() == null ? null : member.getSelectedVibe().getId(),
                        member.getSelectedVibe() == null ? null : member.getSelectedVibe().getLabel(),
                        member.isReady(),
                        member.getPreferenceUpdatedAt()
                ))
                .toList();
        var lastSpin = knownLastSpin != null
                ? knownLastSpin
                : roomSpinRepository.findFirstByRoomIdOrderBySpinNumberDesc(room.getId()).orElse(null);
        RouletteMovieResponse lastMovie = lastSpin == null
                ? null
                : objectMapper.convertValue(lastSpin.getMovieResult(), RouletteMovieResponse.class);

        return new SocialRoomResponse(
                room.getId(),
                room.getRoomType(),
                room.getStatus(),
                room.getInviteCode(),
                room.getOwner().getId(),
                room.getOwner().getDisplayName(),
                room.getOwner().getId().equals(currentUserId),
                room.getRoomType().capacity(),
                memberResponses,
                commonProviders,
                lastMovie,
                lastSpin == null ? 0 : lastSpin.getSpinNumber(),
                room.getUpdatedAt()
        );
    }

    private Map<UUID, List<StreamingProviderEntity>> preferencesByUser(
            List<SocialRoomMemberEntity> members
    ) {
        var userIds = members.stream().map(member -> member.getUser().getId()).toList();
        var result = new LinkedHashMap<UUID, List<StreamingProviderEntity>>();
        userIds.forEach(userId -> result.put(userId, new ArrayList<>()));
        preferenceRepository.findAllWithProviderByUserIdIn(userIds).forEach(preference -> {
            if (preference.getProvider().isActive()) {
                result.get(preference.getUser().getId()).add(preference.getProvider());
            }
        });
        result.values().forEach(providers -> providers.sort(providerComparator()));
        return result;
    }

    private List<ProviderChoice> commonProviders(List<SocialRoomMemberEntity> members) {
        return commonProviders(members, preferencesByUser(members));
    }

    private List<ProviderChoice> commonProviders(
            List<SocialRoomMemberEntity> members,
            Map<UUID, List<StreamingProviderEntity>> preferencesByUser
    ) {
        if (members.isEmpty()) {
            return List.of();
        }
        Map<UUID, StreamingProviderEntity> intersection = preferencesByUser
                .getOrDefault(members.getFirst().getUser().getId(), List.of()).stream()
                .collect(Collectors.toMap(
                        StreamingProviderEntity::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        for (var member : members.subList(1, members.size())) {
            Set<UUID> providerIds = preferencesByUser
                    .getOrDefault(member.getUser().getId(), List.of()).stream()
                    .map(StreamingProviderEntity::getId)
                    .collect(Collectors.toSet());
            intersection.keySet().retainAll(providerIds);
        }
        return intersection.values().stream()
                .sorted(providerComparator())
                .map(provider -> new ProviderChoice(provider, toProviderResponse(provider)))
                .toList();
    }

    private Comparator<StreamingProviderEntity> providerComparator() {
        return Comparator.comparingInt(StreamingProviderEntity::getDisplayPriority)
                .thenComparing(StreamingProviderEntity::getName)
                .thenComparing(StreamingProviderEntity::getId);
    }

    private SocialProviderResponse toProviderResponse(StreamingProviderEntity provider) {
        return new SocialProviderResponse(provider.getId(), provider.getName(), provider.getLogoPath());
    }

    private Map<String, Object> buildFilters(
            RouletteSpinRequest request,
            List<SocialRoomMemberEntity> members
    ) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("providerIds", request.providerIds().stream().map(UUID::toString).sorted().toList());
        filters.put("consensusMode", "EVERY_MEMBER_MATCHES_AT_LEAST_ONE");
        filters.put("memberPreferences", members.stream().map(member -> {
            Map<String, Object> preference = new LinkedHashMap<>();
            preference.put("userId", member.getUser().getId().toString());
            preference.put("genreIds", Arrays.stream(member.getSelectedGenreIds()).sorted().toList());
            preference.put(
                    "vibeId",
                    member.getSelectedVibe() == null
                            ? null
                            : member.getSelectedVibe().getId().toString()
            );
            return preference;
        }).toList());
        return filters;
    }

    private void ensureMembership(UUID roomId, UUID userId) {
        if (!memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new SocialRoomAccessDeniedException("Você não participa desta sala.");
        }
    }

    private void ensureHost(SocialRoomEntity room, UUID userId) {
        ensureMembership(room.getId(), userId);
        if (!room.getOwner().getId().equals(userId)) {
            throw new SocialRoomAccessDeniedException("Somente o anfitrião pode girar a roleta.");
        }
    }

    private void ensureOpen(SocialRoomEntity room) {
        if (room.getStatus() != SocialRoomStatus.OPEN) {
            throw new SocialRoomConflictException("Esta sala já foi encerrada.");
        }
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < MAX_INVITE_ATTEMPTS; attempt++) {
            var code = new StringBuilder(INVITE_LENGTH);
            for (int index = 0; index < INVITE_LENGTH; index++) {
                code.append(INVITE_ALPHABET.charAt(RANDOM.nextInt(INVITE_ALPHABET.length())));
            }
            var candidate = code.toString();
            if (!roomRepository.existsByInviteCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique social room invite code");
    }

    private record ProviderChoice(
            StreamingProviderEntity entity,
            SocialProviderResponse response
    ) {
    }
}
