package com.example.sharepointbatch.sharepoint.batch;

import com.example.sharepointbatch.sharepoint.application.port.out.ListChangedItemsPort;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Primary;

import java.util.LinkedHashMap;
import java.util.Map;

// Test double standing in for a real Graph delta query - there's no way to test against real
// SharePoint (see the plan), so this is the whole point of the reader/processor/writer boundary.
// @Primary because the real GraphChangedItemsAdapter is still component-scanned into the test
// context too (it lives under the same base package) - this is what wins the ambiguity rather
// than excluding the real adapter's package from scanning.
// Stateful/mutable: tests populate it with canned pages keyed by the link they expect to be
// requested with, then reset() between test methods since the Spring test context caches and
// reuses this bean across the class.
@TestComponent
@Primary
class FakeListChangedItemsPort implements ListChangedItemsPort {

    private final Map<String, DeltaPage> pagesByLink = new LinkedHashMap<>();

    void whenLink(String link, DeltaPage page) {
        pagesByLink.put(link, page);
    }

    void reset() {
        pagesByLink.clear();
    }

    @Override
    public DeltaPage list(String link) {
        DeltaPage page = pagesByLink.get(link);
        if (page == null) {
            throw new IllegalStateException("FakeListChangedItemsPort has no page configured for link: " + link);
        }
        return page;
    }
}
