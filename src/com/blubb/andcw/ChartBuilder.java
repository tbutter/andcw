/**
 * Copyright (C) 2012 Thomas Butter
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blubb.andcw;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.Metric;

public class ChartBuilder extends Activity {
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

	private GraphicalView mChartView;

	private AWSCredentials getAWSCredentials() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (prefs.getString("access_key", "XX").equals("XX"))
			return null;
		return new BasicAWSCredentials(prefs.getString("access_key", "XX"),
				prefs.getString("secret_key", "XX"));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Metric m = new Metric().withMetricName(
				(String) getIntent().getExtras().get("metric")).withNamespace(
				(String) getIntent().getExtras().get("namespace"));
		Dimension d = new Dimension().withName(
				(String) getIntent().getExtras().get("dimension")).withValue(
				(String) getIntent().getExtras().get("dimensionvalue"));
		TimeSeries series = new TimeSeries(m.getMetricName() + " - "
				+ d.getName() + " " + d.getValue());
		AWSCredentials awsc = getAWSCredentials();
		AmazonCloudWatchAsyncClient acw = new AmazonCloudWatchAsyncClient(awsc);
		long offset = new Date().getTime() - 1000 * 3600 * 24;
		GetMetricStatisticsRequest gmsr = new GetMetricStatisticsRequest()
				.withStartTime(new Date(offset))
				.withMetricName(m.getMetricName())
				.withNamespace(m.getNamespace()).withDimensions(d)
				.withPeriod(300).withStatistics("Average")
				.withEndTime(new Date());
		Log.i("AndCW req", gmsr.toString());
		Future<GetMetricStatisticsResult> res = acw
				.getMetricStatisticsAsync(gmsr);
		TreeSet<Datapoint> data = new TreeSet<Datapoint>(
				new Comparator<Datapoint>() {

					public int compare(Datapoint lhs, Datapoint rhs) {
						return lhs.getTimestamp().compareTo(rhs.getTimestamp());
					}
				});
		try {
			data.addAll((List<Datapoint>) res.get().getDatapoints()); // FIXME
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		for (Datapoint dp : data) {
			series.add(dp.getTimestamp(), dp.getAverage());
		}
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		mRenderer.addSeriesRenderer(renderer);
		renderer.setPointStyle(PointStyle.CIRCLE);
		renderer.setFillPoints(true);
		mDataset.addSeries(series);
		mRenderer.setApplyBackgroundColor(true);
		mRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
		mRenderer.setAxisTitleTextSize(16);
		mRenderer.setChartTitleTextSize(20);
		mRenderer.setLabelsTextSize(15);
		mRenderer.setLegendTextSize(15);
		mRenderer.setMargins(new int[] { 20, 30, 15, 0 });
		mRenderer.setZoomButtonsVisible(true);
		mRenderer.setPointSize(1);
		mChartView = ChartFactory.getTimeChartView(this, mDataset, mRenderer,
				null);
		setContentView(mChartView);

	}
}