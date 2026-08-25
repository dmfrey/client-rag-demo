package com.example.sharepointbatch.sharepoint.batch;

import com.example.sharepointbatch.sharepoint.application.domain.model.ChangedItem;
import com.example.sharepointbatch.sharepoint.application.port.out.ListChangedItemsPort;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Pages through Graph's delta query via ListChangedItemsPort, starting from the link
 * ResolveDeltaLinkTasklet put into the job's execution context (step-scoped late binding via the
 * #{jobExecutionContext[...]} SpEL expression below - Spring Batch resolves it once this step
 * actually starts, by which point Step 1 has already run). When the final page (carrying a
 * deltaLink rather than a nextLink) is reached, the new link is written into THIS STEP's own
 * execution context via the ItemStream#update callback; SharePointSyncJobConfig registers an
 * ExecutionContextPromotionListener on the step to copy it up to job level afterward, so
 * PersistDeltaLinkTasklet (Step 3) can read it back the same way Step 1's value was read here.
 */
@Component
@StepScope
class ChangedItemReader implements ItemStreamReader<ChangedItem> {

    static final String FINAL_DELTA_LINK_KEY = "sharePointFinalDeltaLink";

    private final ListChangedItemsPort listChangedItemsPort;
    private final Deque<ChangedItem> buffer = new ArrayDeque<>();
    private String currentLink;
    private String finalDeltaLink;
    private boolean exhausted;

    ChangedItemReader(ListChangedItemsPort listChangedItemsPort,
                       @Value("#{jobExecutionContext['" + ResolveDeltaLinkTasklet.START_LINK_KEY + "']}") String startLink) {
        this.listChangedItemsPort = listChangedItemsPort;
        this.currentLink = startLink;
    }

    @Override
    public ChangedItem read() {
        if (buffer.isEmpty() && !exhausted) {
            fetchNextPage();
        }
        return buffer.poll();
    }

    @Override
    public void open(ExecutionContext executionContext) {
    }

    @Override
    public void update(ExecutionContext executionContext) {
        if (finalDeltaLink != null) {
            executionContext.putString(FINAL_DELTA_LINK_KEY, finalDeltaLink);
        }
    }

    @Override
    public void close() throws ItemStreamException {
    }

    private void fetchNextPage() {
        if (currentLink == null) {
            exhausted = true;
            return;
        }

        ListChangedItemsPort.DeltaPage page = listChangedItemsPort.list(currentLink);
        buffer.addAll(page.items());

        if (page.deltaLink() != null) {
            finalDeltaLink = page.deltaLink();
            currentLink = null;
        }
        else {
            currentLink = page.nextLink();
        }

        if (buffer.isEmpty() && currentLink == null) {
            exhausted = true;
        }
    }
}
