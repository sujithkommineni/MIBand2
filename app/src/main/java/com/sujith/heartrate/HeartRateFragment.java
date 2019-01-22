package com.sujith.heartrate;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.sujith.heartrate.btconnection.BTDeviceConnection;
import com.sujith.heartrate.btconnection.BTDeviceManager;
import com.sujith.heartrate.btconnection.data.ResponseData;

import timber.log.Timber;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HeartRateFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HeartRateFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HeartRateFragment extends Fragment implements View.OnClickListener, BTDeviceManager.BTDeviceConnectivityListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    private Button mButtonHeartRate;
    private BTDeviceConnection mBTConnection;

    private OnFragmentInteractionListener mListener;
    private BTDeviceManager mBtManager;
    private View mProgressBar;
    private TextView mTextDeviceInfo, mTextHeartRate;

    public HeartRateFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HeartRateFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HeartRateFragment newInstance(String param1, String param2) {
        HeartRateFragment fragment = new HeartRateFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_heart_rate, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mButtonHeartRate = view.findViewById(R.id.button_heart_rate);
        mButtonHeartRate.setOnClickListener(this);
        mProgressBar = view.findViewById(R.id.layout_progress);
        mTextDeviceInfo = view.findViewById(R.id.tv_device_info);
        mTextHeartRate = view.findViewById(R.id.tv_heart_rate);

        mBtManager = BTDeviceManager.getInstance(getActivity().getApplicationContext());
        mBtManager.scanForLeDevice(this);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        if (view == mButtonHeartRate) {
            triggerHeartRate();
        }
    }

    private void triggerHeartRate() {
        if (mBTConnection == null) {
            Timber.e("BT connection is not initialized..");
            return;
        }

        mBTConnection.getHeartRate(new BTDeviceConnection.ResponseListener() {
            @Override
            public void onResponse(ResponseData response) {
                Timber.d("onResponse, getLogData, success : %s", response.success);
                if (getActivity() == null) return;
                if (response.success) {
                    mTextHeartRate.setText(response.msg);
                }

            }
        });
    }

    @Override
    public void onDeviceConnectionStateChange(int state, BTDeviceConnection connection) {
        Timber.d("onDeviceConnectionStateChange()..");
        if (state == BTDeviceManager.CONNECTIVIY_PROGRESS) {
            mBTConnection = null;
            mProgressBar.setVisibility(View.VISIBLE);
        } else if (state == BTDeviceManager.CONNECTIVIY_CONNECTED) {
            mBTConnection = connection;
            updateUiForConnected();
        } else { // NOT CONNECTED
            mBTConnection = null;
            updateUiForDisconnected();
        }
    }


    private void updateUiForDisconnected() {
        mProgressBar.setVisibility(View.GONE);
        mTextDeviceInfo.setText(R.string.not_connected);
    }

    private void updateUiForConnected() {
        mProgressBar.setVisibility(View.GONE);
        mTextDeviceInfo.setText(mBtManager.getDeviceName());
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
