package com.maximys777.project.watchlist.tvshow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_tv_shows")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistTvShowEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tvShowId;

    private String name;

    private String posterUrl;

    private String firstAirDate;

    private String originCountry;

    private Integer currentSeason;

    private Integer currentEpisode;

    @CreationTimestamp
    private LocalDateTime addedDate;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
