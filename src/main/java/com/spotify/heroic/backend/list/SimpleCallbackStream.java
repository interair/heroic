package com.spotify.heroic.backend.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.extern.slf4j.Slf4j;

import com.spotify.heroic.async.Callback;
import com.spotify.heroic.async.CallbackStream;
import com.spotify.heroic.async.CancelReason;
import com.spotify.heroic.backend.BackendManager.DataPointGroup;
import com.spotify.heroic.backend.BackendManager.QueryMetricsResult;
import com.spotify.heroic.backend.RowStatistics;
import com.spotify.heroic.backend.model.FetchDataPoints;
import com.spotify.heroic.model.DataPoint;
import com.spotify.heroic.model.TimeSerie;
import com.spotify.heroic.model.TimeSerieSlice;

@Slf4j
public final class SimpleCallbackStream implements
        Callback.StreamReducer<FetchDataPoints.Result, QueryMetricsResult> {
    private final TimeSerieSlice slice;

    private final Queue<FetchDataPoints.Result> results = new ConcurrentLinkedQueue<FetchDataPoints.Result>();

    SimpleCallbackStream(TimeSerieSlice slice) {
        this.slice = slice;
    }

    @Override
    public void finish(CallbackStream<FetchDataPoints.Result> stream,
            Callback<FetchDataPoints.Result> callback,
            FetchDataPoints.Result result) throws Exception {
        results.add(result);
    }

    @Override
    public void error(CallbackStream<FetchDataPoints.Result> stream,
            Callback<FetchDataPoints.Result> callback, Throwable error)
            throws Exception {
        log.error("Result failed: " + error, error);
    }

    @Override
    public void cancel(CallbackStream<FetchDataPoints.Result> stream,
            Callback<FetchDataPoints.Result> callback, CancelReason reason)
            throws Exception {
    }

    @Override
    public QueryMetricsResult done(int successful, int failed, int cancelled)
            throws Exception {
        final List<DataPoint> datapoints = joinRawResults();
        final TimeSerie timeSerie = slice.getTimeSerie();

        final RowStatistics rowStatistics = new RowStatistics(successful,
                failed, cancelled);

        final List<DataPointGroup> groups = new ArrayList<DataPointGroup>();
        groups.add(new DataPointGroup(timeSerie.getTags(), datapoints));

        return new QueryMetricsResult(groups, datapoints.size(), 0,
                rowStatistics);
    }

    private List<DataPoint> joinRawResults() {
        final List<DataPoint> datapoints = new ArrayList<DataPoint>();

        for (final FetchDataPoints.Result result : results) {
            datapoints.addAll(result.getDatapoints());
        }

        Collections.sort(datapoints);
        return datapoints;
    }
}