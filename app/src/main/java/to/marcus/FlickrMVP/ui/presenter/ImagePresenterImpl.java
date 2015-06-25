package to.marcus.FlickrMVP.ui.presenter;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import to.marcus.FlickrMVP.data.PhotoFactory;
import to.marcus.FlickrMVP.data.event.ImagesReceivedEvent;
import to.marcus.FlickrMVP.data.event.ImagesRequestedEvent;
import to.marcus.FlickrMVP.model.Photos;
import to.marcus.FlickrMVP.network.PhotoHandler;
import to.marcus.FlickrMVP.ui.adapter.PhotoAdapter;
import to.marcus.FlickrMVP.ui.views.PhotosView;

/**
* Created by marcus on 15/04/15
*/

public class ImagePresenterImpl implements ImagePresenter{
    private final String TAG = ImagePresenterImpl.class.getSimpleName();
    private final Bus bus;
    private PhotosView view;
    private Photos defaultPhotosArray;
    PhotoHandler mResponseHandler;

    public ImagePresenterImpl(PhotosView view, Bus bus){
        this.bus = bus;
        this.view = view;
        initResponseHandler();
    }

    //to-do
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        Log.i(TAG, "on activity created");
        if(savedInstanceState == null){
            //view.showprogressbar
            initInstanceState();
            requestNetworkPhotos("search term");
        }else{
            //pullImagesFromCache --need to setup LRU
            restoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out){
        Log.i(TAG, "on save instance state");
        Photos.putParcelableArray(out, defaultPhotosArray);
    }

    private void restoreInstanceState(Bundle savedInstanceState){
        Log.i(TAG, "on restore instance state");
        defaultPhotosArray = Photos.getParcelableArray(savedInstanceState);
        initGridViewAdapter();
    }

    @Override
    public void onResume(){
        bus.register(this);
    }

    @Override
    public void onPause(){
        bus.unregister(this);
    }

    @Override
    public void onRefresh(){}

    @Override
    public void onDestroy(){}

    @Override
    public void requestNetworkPhotos(String request){
        //view.showloadingIndicator  -- HomeFragment will implement our view interface and override this method
       // Log.i(TAG, "Images Requested Event!");
        bus.post(new ImagesRequestedEvent(request));  //this will notify our ApiRequestHandler
    }

    @Subscribe
    public void onImagesArrayReceived(ImagesReceivedEvent event){
        Log.i(TAG, "array ready for presenter");
        this.defaultPhotosArray.setPhotos(event.getResult());
        initGridViewAdapter();
    }

    private void initInstanceState(){
        defaultPhotosArray = PhotoFactory.Photos.initDefaultPhotosArray();
    }

    private void initResponseHandler(){
        mResponseHandler = new PhotoHandler<android.widget.ImageView>(new Handler());
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