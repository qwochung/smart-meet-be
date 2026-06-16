package com.example.smartmeetbe.merge;

import com.example.smartmeetbe.constant.MergeStatus;
import com.example.smartmeetbe.dto.response.TranscriptSegmentDto;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class RoomMergeState {

    private final String roomId;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, ParticipantMergeState> participants = new ConcurrentHashMap<>();
    private final List<MergeSegment> timeline = new ArrayList<>();
    private final Set<String> processedChunkIds = new HashSet<>();

    @Setter
    private int version = 0;

    @Setter
    private String fullTextCache = "";

    @Setter
    private long lastActivityAt = System.currentTimeMillis();

    @Setter
    private int lastPersistedVersion = -1;

    @Setter
    private MergeStatus status = MergeStatus.LIVE;

    @Setter
    private String lastProcessedChunkId;

    @Setter
    private boolean hydrated = false;

    public RoomMergeState(String roomId) {
        this.roomId = roomId;
    }

    public boolean needsPersist(int everyNVersions) {
        return version > lastPersistedVersion
                && (version % everyNVersions == 0 || version - lastPersistedVersion >= everyNVersions);
    }
}
