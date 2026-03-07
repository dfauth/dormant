package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.Watchlist;
import io.github.dfauth.trade.model.WatchlistItem;
import io.github.dfauth.trade.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;

    @Transactional
    public Watchlist batchUpsert(Long userId, String name, List<WatchlistItem> items) {
        Watchlist wl = watchlistRepository.findByUserIdAndName(userId, name)
                .orElseGet(() -> watchlistRepository.save(
                        Watchlist.builder().userId(userId).name(name).build()));
        // remove those missing from the submitted list
        new ArrayList(wl.getItems()).stream().filter(i -> !items.contains(i)).forEach(i -> wl.getItems().remove(i));

        // add those from the submitted list that are missing from the db list
        items.stream().filter(i -> !wl.getItems().contains(i)).forEach(i -> {
            i.setWatchlist(wl);
            wl.getItems().add(i);
        });
        return watchlistRepository.save(wl);
    }
}
