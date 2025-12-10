package com.bar.voiceday;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView scheduleRecyclerView;
    private RecyclerView sitesRecyclerView;
    private ScheduleAdapter scheduleAdapter;
    private SitesAdapter sitesAdapter;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public HomeFragment() {
        // Required empty public constructor
    }

    public static Fragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // 오늘의 일정 RecyclerView 설정
        scheduleRecyclerView = view.findViewById(R.id.scheduleRecyclerView);
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false
        ));
        scheduleAdapter = new ScheduleAdapter();
        scheduleRecyclerView.setAdapter(scheduleAdapter);

        // 자주 방문하는 사이트 RecyclerView 설정
        sitesRecyclerView = view.findViewById(R.id.sitesRecyclerView);
        sitesRecyclerView.setLayoutManager(new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false
        ));
        sitesAdapter = new SitesAdapter();
        sitesRecyclerView.setAdapter(sitesAdapter);

        loadTodaySchedules();
        loadFrequentSites();
    }

    private void loadTodaySchedules() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN);
        String today = sdf.format(new Date());

        db.collection("schedules")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        return;
                    }

                    List<Schedule> schedules = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String startDate = doc.getString("startDate");
                            String endDate = doc.getString("endDate");
                            Boolean allDay = doc.getBoolean("allDay");
                            String repeat = doc.getString("repeat");

                            if (startDate == null) startDate = "";
                            if (endDate == null) endDate = "";
                            if (allDay == null) allDay = false;
                            if (repeat == null) repeat = "";

                            // 오늘 일정인지 확인
                            if (isScheduleForToday(startDate, endDate, repeat, today)) {
                                Schedule schedule = new Schedule(
                                        doc.getString("id") != null ? doc.getString("id") : doc.getId(),
                                        doc.getString("title") != null ? doc.getString("title") : "",
                                        doc.getString("place") != null ? doc.getString("place") : "",
                                        doc.getString("startTime") != null ? doc.getString("startTime") : "",
                                        doc.getString("endTime") != null ? doc.getString("endTime") : "",
                                        allDay,
                                        startDate,
                                        endDate,
                                        doc.getString("memo") != null ? doc.getString("memo") : "",
                                        repeat
                                );
                                schedules.add(schedule);
                            }
                        }
                    }

                    // 시간순 정렬 (종일 일정은 맨 앞에)
                    Collections.sort(schedules, new Comparator<Schedule>() {
                        @Override
                        public int compare(Schedule s1, Schedule s2) {
                            if (s1.allDay && !s2.allDay) return -1;
                            if (!s1.allDay && s2.allDay) return 1;
                            return s1.startTime.compareTo(s2.startTime);
                        }
                    });

                    scheduleAdapter.submitList(schedules);
                });
    }

    private boolean isScheduleForToday(String startDate, String endDate, String repeat, String today) {
        // 단순 날짜 비교
        if (startDate.compareTo(today) <= 0 && today.compareTo(endDate) <= 0) {
            return true;
        }

        // 반복 일정 처리
        if (!repeat.isEmpty() && !repeat.equals("반복 안 함")) {
            return checkRepeatSchedule(startDate, repeat, today);
        }

        return false;
    }

    private boolean checkRepeatSchedule(String startDate, String repeat, String today) {
        Calendar startCal = parseDate(startDate);
        Calendar todayCal = parseDate(today);

        if (startCal == null || todayCal == null) {
            return false;
        }

        // 시작일보다 이전이면 false
        if (startDate.compareTo(today) > 0) {
            return false;
        }

        switch (repeat) {
            case "매일":
                return true;

            case "매주":
                return startCal.get(Calendar.DAY_OF_WEEK) == todayCal.get(Calendar.DAY_OF_WEEK);

            case "매월":
                return startCal.get(Calendar.DAY_OF_MONTH) == todayCal.get(Calendar.DAY_OF_MONTH);

            case "매년":
                return startCal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH) &&
                        startCal.get(Calendar.DAY_OF_MONTH) == todayCal.get(Calendar.DAY_OF_MONTH);

            default:
                return false;
        }
    }

    private Calendar parseDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN);
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        } catch (Exception e) {
            return null;
        }
    }

    private void loadFrequentSites() {
        db.collection("frequentSites")
                .orderBy("visitCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        return;
                    }

                    List<FrequentSite> sites = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            FrequentSite site = new FrequentSite(
                                    doc.getId(),
                                    doc.getString("title") != null ? doc.getString("title") : "노션",
                                    doc.getString("url") != null ? doc.getString("url") : "",
                                    doc.getString("imageUrl") != null ? doc.getString("imageUrl") : ""
                            );
                            sites.add(site);
                        }
                    }
                    sitesAdapter.submitList(sites);
                });
    }

    // 데이터 클래스
    public static class Schedule {
        public String id;
        public String title;
        public String place;
        public String startTime;
        public String endTime;
        public boolean allDay;
        public String startDate;
        public String endDate;
        public String memo;
        public String repeat;

        public Schedule(String id, String title, String place, String startTime,
                        String endTime, boolean allDay, String startDate, String endDate,
                        String memo, String repeat) {
            this.id = id;
            this.title = title;
            this.place = place;
            this.startTime = startTime;
            this.endTime = endTime;
            this.allDay = allDay;
            this.startDate = startDate;
            this.endDate = endDate;
            this.memo = memo;
            this.repeat = repeat;
        }
    }

    public static class FrequentSite {
        public String id;
        public String title;
        public String url;
        public String imageUrl;

        public FrequentSite(String id, String title, String url, String imageUrl) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.imageUrl = imageUrl;
        }
    }

    // 오늘의 일정 어댑터
    public static class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

        private List<Schedule> schedules = new ArrayList<>();

        public void submitList(List<Schedule> list) {
            this.schedules = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_schedule_horizontal, parent, false);
            return new ScheduleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
            holder.bind(schedules.get(position));
        }

        @Override
        public int getItemCount() {
            return schedules.size();
        }

        public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
            private TextView timeText;
            private TextView titleText;
            private TextView locationText;
            private View colorIndicator;

            public ScheduleViewHolder(@NonNull View itemView) {
                super(itemView);
                timeText = itemView.findViewById(R.id.timeText);
                titleText = itemView.findViewById(R.id.titleText);
                locationText = itemView.findViewById(R.id.locationText);
                colorIndicator = itemView.findViewById(R.id.colorIndicator);
            }

            public void bind(Schedule schedule) {
                // 시간 표시
                if (schedule.allDay) {
                    timeText.setText("종일");
                } else {
                    String timeDisplay;
                    if (!schedule.startTime.isEmpty() && !schedule.endTime.isEmpty()) {
                        timeDisplay = schedule.startTime + " - " + schedule.endTime;
                    } else if (!schedule.startTime.isEmpty()) {
                        timeDisplay = schedule.startTime;
                    } else {
                        timeDisplay = "시간 미정";
                    }
                    timeText.setText(timeDisplay);
                }

                titleText.setText(schedule.title);

                // 장소 표시
                if (!schedule.place.isEmpty()) {
                    locationText.setVisibility(View.VISIBLE);
                    locationText.setText(schedule.place);
                } else {
                    locationText.setVisibility(View.GONE);
                }
            }
        }
    }

    // 자주 방문하는 사이트 어댑터
    public static class SitesAdapter extends RecyclerView.Adapter<SitesAdapter.SiteViewHolder> {

        private List<FrequentSite> sites = new ArrayList<>();

        public void submitList(List<FrequentSite> list) {
            this.sites = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SiteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_site, parent, false);
            return new SiteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SiteViewHolder holder, int position) {
            holder.bind(sites.get(position));
        }

        @Override
        public int getItemCount() {
            return sites.size();
        }

        public static class SiteViewHolder extends RecyclerView.ViewHolder {
            private ImageView siteImage;
            private TextView siteName;

            public SiteViewHolder(@NonNull View itemView) {
                super(itemView);
                siteImage = itemView.findViewById(R.id.siteImage);
                siteName = itemView.findViewById(R.id.siteName);
            }

            public void bind(FrequentSite site) {
                siteName.setText(site.title);

                // Glide 또는 다른 이미지 로딩 라이브러리 사용
                // Glide.with(itemView.getContext())
                //     .load(site.imageUrl)
                //     .placeholder(R.drawable.ic_website_placeholder)
                //     .into(siteImage);

                itemView.setOnClickListener(v -> {
                    if (!site.url.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(site.url));
                        itemView.getContext().startActivity(intent);
                    }
                });
            }
        }
    }
}