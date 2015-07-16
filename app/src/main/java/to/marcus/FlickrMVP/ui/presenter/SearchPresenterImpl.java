package to.marcus.FlickrMVP.ui.presenter;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import to.marcus.FlickrMVP.data.PhotoCache;
import to.marcus.FlickrMVP.data.PhotoFactory;
import to.marcus.FlickrMVP.data.event.SearchReceivedEvent;
import to.marcus.FlickrMVP.data.event.SearchRequestedEvent;
import to.marcus.FlickrMVP.model.Photos;
import to.marcus.FlickrMVP.network.PhotoHandler;
import to.marcus.FlickrMVP.ui.adapter.PhotoAdapter;
import to.marcus.FlickrMVP.ui.views.PhotosView;

/**
 * Created by marcus on 6/26/2015
 */

public class SearchPresenterImpl implements SearchPresenter {
    private final String TAG = SearchPresenterImpl.class.getSimpleName();
    private PhotoCache photoCache;
    private final Bus bus;
    private PhotosView view;
    private Photos defaultPhotosArray;
    PhotoHandler mResponseHandler;

    public SearchPresenterImpl(PhotosView view, Bus bus, PhotoCache photoCache) {
        this.photoCache = photoCache;
        this.bus = bus;
        this.view = view;
        initResponseHandler();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if(savedInstanceState == null){
            view.showProgressBar();
            initInstanceState();
        }else{
            restoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        Photos.putParcelableArray(out, defaultPhotosArray);
    }

    private void restoreInstanceState(Bundle savedInstanceState){
        defaultPhotosArray = Photos.getParcelableArray(savedInstanceState);
        initGridViewAdapter();
    }

    @Override
    public void onResume() {
        bus.register(this);
    }

    @Override
    public void onPause() {
        bus.unregister(this);
    }

    @Override
    public void onDestroy() {}

    @Override
    public void onRefresh() {}

    @Override
    public void requestNetworkPhotos(String query) {
        bus.post(new SearchRequestedEvent(query));
    }

    @Subscribe
    public void onImagesArrayReceived(SearchReceivedEvent event){
        this.defaultPhotosArray.setPhotos(event.getResult());
        initGridViewAdapter();
        view.hideProgressBar();
    }

    private void initInstanceState(){
        defaultPhotosArray = PhotoFactory.Photos.initDefaultPhotosArray();
    }


    private void initResponseHandler() {
        mResponseHandler = new PhotoHandler<android.widget.ImageView>(new Handler(), photoCache);
        mResponseHandler.start();
        mResponseHandler.getLooper();
        //listen for incoming images sent back to main handler thread
        mResponseHandler.setListener(new PhotoHandler.Listener<android.widget.ImageView>() {
            public void onPhotoDownloaded(android.widget.ImageView imageView, Bitmap thumbnail) {
                imageView.setImageBitmap(thumbnail);
                Log.i(TAG, "ResponseHandler: imageview set thumbnail");
            }
        });
    }

    private void initGridViewAdapter(){
        //to-do : determine which photosArray to use (cached / network?)
        view.setGridViewAdapter(new PhotoAdapter(view.getContext(), defaultPhotosArray.getPhotos(), mResponseHandler));
    }
}
