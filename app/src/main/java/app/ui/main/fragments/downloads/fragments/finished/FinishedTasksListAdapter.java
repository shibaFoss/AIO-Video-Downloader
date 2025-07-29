package app.ui.main.fragments.downloads.fragments.finished;

import static android.view.LayoutInflater.from;
import static com.aio.R.layout;
import static lib.files.FileSystemUtility.addToMediaStore;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;

import java.io.File;

import app.core.AIOApp;
import app.core.engines.downloader.DownloadDataModel;
import app.core.engines.downloader.DownloadSystem;

/**
 * Adapter class for displaying finished download tasks in a ListView.
 * It binds {@link DownloadDataModel} items to the corresponding list item layout using
 * {@link FinishedTasksViewHolder}. Automatically updates the MediaStore for newly finished files.
 */
public class FinishedTasksListAdapter extends BaseAdapter {

    private final FinishedTasksFragment finishedTasksFragment;
    private final LayoutInflater layoutInflater;
    private final DownloadSystem downloadSystem;
    private int existingTaskCount;

    /**
     * Constructs a new FinishedTasksListAdapter with the given fragment.
     *
     * @param fragment the fragment containing the finished download list
     */
    public FinishedTasksListAdapter(@NonNull FinishedTasksFragment fragment) {
        this.finishedTasksFragment = fragment;
        this.layoutInflater = from(fragment.getContext());
        this.downloadSystem = AIOApp.INSTANCE.getDownloadManager();
    }

    /**
     * Returns the total number of finished downloads.
     */
    @Override
    public int getCount() {
        return downloadSystem.getFinishedDownloadDataModels().size();
    }

    /**
     * Returns the finished download item at the specified position.
     *
     * @param index the position of the item
     * @return the corresponding {@link DownloadDataModel}
     */
    @Override
    public DownloadDataModel getItem(int index) {
        return downloadSystem.getFinishedDownloadDataModels().get(index);
    }

    /**
     * Returns the ID of the item at the specified position.
     *
     * @param index the item index
     * @return the item ID (in this case, same as index)
     */
    @Override
    public long getItemId(int index) {
        return index;
    }

    /**
     * Returns the view for a list item at a specific position.
     * Uses {@link FinishedTasksViewHolder} to bind data.
     *
     * @param position    the item position
     * @param convertView an existing view to reuse if available
     * @param parent      the parent view group
     * @return the fully bound item view
     */
    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(layout.frag_down_4_finish_1_row_1, null);
        }
        verifyAndUpdateViewHolder(convertView, position);
        return convertView;
    }

    /**
     * Notifies that the dataset has changed and updates MediaStore if necessary.
     */
    @Override
    public void notifyDataSetChanged() {
        int newTaskCount = finishedTasksFragment.getFinishedDownloadModels().size();
        if (newTaskCount != existingTaskCount) {
            super.notifyDataSetChanged();
            existingTaskCount = getCount();
            updateMediaStore();
        }
    }

    /**
     * Adds all finished downloads to the Android MediaStore so they appear in galleries and media apps.
     */
    private void updateMediaStore() {
        try {
            int index = 0;
            while (index < getCount()) {
                DownloadDataModel model = getItem(index);
                File downloadedFile = model.getDestinationFile();
                addToMediaStore(downloadedFile);
                index++;
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    /**
     * Forces or safely updates the list view when sorting is applied.
     *
     * @param isForceRefresh true to call {@code super.notifyDataSetChanged()}, false to use regular notify
     */
    public void notifyDataSetChangedOnSort(Boolean isForceRefresh) {
        try {
            if (isForceRefresh) super.notifyDataSetChanged();
            else notifyDataSetChanged();
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    /**
     * Verifies the ViewHolder tag for the row and updates it with the corresponding data.
     *
     * @param rowLayout the list item view
     * @param position  the item index
     */
    private void verifyAndUpdateViewHolder(View rowLayout, int position) {
        FinishedTasksViewHolder viewHolder;
        if (rowLayout.getTag() == null) {
            viewHolder = new FinishedTasksViewHolder(rowLayout);
            rowLayout.setTag(viewHolder);
        } else viewHolder = (FinishedTasksViewHolder) rowLayout.getTag();
        viewHolder.updateView(getItem(position), finishedTasksFragment);
    }
}