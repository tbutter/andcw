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

import java.util.concurrent.Future;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;

public class AndCWActivity extends ListActivity {

	private AWSCredentials getAWSCredentials() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (prefs.getString("access_key", "XX").equals("XX"))
			return null;
		return new BasicAWSCredentials(prefs.getString("access_key", "XX"),
				prefs.getString("secret_key", "XX"));
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setListAdapter(new MetricsListAdapter());

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(AndCWActivity.this,
						ChartBuilder.class);
				intent.putExtra("metric",
						((Metric) getListAdapter().getItem(position))
								.getMetricName());

				intent.putExtra(
						"dimension",
						((Dimension) ((Metric) getListAdapter().getItem(
								position)).getDimensions().get(0)).getName());
				intent.putExtra(
						"dimensionvalue",
						((Dimension) ((Metric) getListAdapter().getItem(
								position)).getDimensions().get(0)).getValue());
				intent.putExtra("namespace", ((Metric) getListAdapter()
						.getItem(position)).getNamespace());
				startActivity(intent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.aboutMenu:
			Intent about = new Intent(this, About.class);
			startActivity(about);
			break;
		}
		return true;
	}

	public class MetricsListAdapter extends BaseAdapter {
		ListMetricsResult res;

		public MetricsListAdapter() {
			try {
				AmazonCloudWatchAsyncClient acw = new AmazonCloudWatchAsyncClient(
						getAWSCredentials());
				if (acw != null) {
					Future<ListMetricsResult> fres = acw
							.listMetricsAsync(new ListMetricsRequest());
					res = fres.get(); // FIXME
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (res == null) {
				AndCWActivity.this.startActivity(new Intent(AndCWActivity.this,
						Prefs.class));
				AndCWActivity.this.finish();
			}
		}

		public int getCount() {
			if (res == null)
				return 0;
			return res.getMetrics().size();
		}

		public Object getItem(int position) {
			return res.getMetrics().get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv = new TextView(AndCWActivity.this);
			tv.setPadding(0, 10, 0, 10);
			Metric m = (Metric) getItem(position);
			Dimension d = (Dimension) m.getDimensions().get(0);
			tv.setText(m.getMetricName() + " - " + d.getName() + " "
					+ d.getValue());
			return tv;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			if (res == null)
				return true;
			if (res.getMetrics().isEmpty())
				return true;
			return false;
		}

		public void registerDataSetObserver(DataSetObserver observer) {
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
		}

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int position) {
			return true;
		}
	}
}