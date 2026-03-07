package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM WatchlistItem i WHERE i.id = :itemId AND i.watchlist.id = :watchlistId")
    int deleteByWatchlistIdAndItemId(@Param("watchlistId") Long watchlistId, @Param("itemId") Long itemId);
}
