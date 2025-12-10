package com.bar.voiceday;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
    private LinearLayout weekDatesLayout;
    private TextView userNameText;
    private TextView currentMonthText;

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

        // 현재 월 표시
        currentMonthText = view.findViewById(R.id.currentMonthText);
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy년 MM월", Locale.KOREAN);
        currentMonthText.setText(monthFormat.format(new Date()));

        // 주간 날짜 표시
        weekDatesLayout = view.findViewById(R.id.weekDatesLayout);
        setupWeekDates();

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

        // + 버튼 클릭 리스너 추가
        TextView addSiteButton = view.findViewById(R.id.addSiteButton);
        addSiteButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddSiteActivity.class);
            startActivity(intent);
        });

        loadTodaySchedules();
        loadFrequentSites();
    }

    private void setupWeekDates() {
        weekDatesLayout.removeAllViews();

        Calendar calendar = Calendar.getInstance();

        // 이번 주 일요일로 이동
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        calendar.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - 1));

        int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);

        // 7일 생성 (일요일부터 토요일까지)
        for (int i = 0; i < 7; i++) {
            View dateView = createDateView(calendar, today, currentMonth);
            weekDatesLayout.addView(dateView);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private View createDateView(Calendar calendar, int today, int currentMonth) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        LinearLayout dateLayout = new LinearLayout(getContext());
        dateLayout.setOrientation(LinearLayout.VERTICAL);
        dateLayout.setGravity(android.view.Gravity.CENTER);
        dateLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        ));
        dateLayout.setPadding(8, 8, 8, 8);

        // 날짜 TextView
        TextView dateText = new TextView(getContext());
        int date = calendar.get(Calendar.DAY_OF_MONTH);
        dateText.setText(String.valueOf(date));
        dateText.setTextSize(16);
        dateText.setGravity(android.view.Gravity.CENTER);

        // 오늘 날짜 스타일링
        boolean isToday = (date == today && calendar.get(Calendar.MONTH) == currentMonth);
        if (isToday) {
            // 오늘 날짜는 파란 원형 배경
            dateText.setTextColor(0xFFFFFFFF); // 흰색
            dateText.setBackgroundResource(R.drawable.bg_today_circle);
            int padding = dpToPx(8);
            dateText.setPadding(padding, padding, padding, padding);
        } else {
            dateText.setTextColor(0xFF000000); // 검은색
        }

        dateLayout.addView(dateText);

        return dateLayout;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
        private FirebaseFirestore db = FirebaseFirestore.getInstance();

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
            holder.bind(sites.get(position), db);
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

            public void bind(FrequentSite site, FirebaseFirestore db) {
                siteName.setText(site.title);

                // 웹사이트 미리보기 이미지 로드
                loadWebsitePreview(site);

                itemView.setOnClickListener(v -> {
                    if (!site.url.isEmpty()) {
                        // 방문 횟수 증가
                        incrementVisitCount(site.id, db);

                        // URL로 이동
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(site.url));
                            itemView.getContext().startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(itemView.getContext(),
                                    "링크를 열 수 없습니다: " + site.url,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            private void loadWebsitePreview(FrequentSite site) {
                // 이미 저장된 이미지 URL이 있으면 사용
                if (site.imageUrl != null && !site.imageUrl.isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(site.imageUrl)
                            .placeholder(R.drawable.ic_person_hello)
                            .error(R.drawable.ic_person_hello)
                            .into(siteImage);
                } else {
                    // 웹사이트 스크린샷 API 사용
                    String screenshotUrl = getScreenshotUrl(site.url);

                    Glide.with(itemView.getContext())
                            .load(screenshotUrl)
                            .placeholder(R.drawable.ic_person_hello)
                            .error(R.drawable.ic_person_hello)
                            .into(siteImage);
                }
            }

            private String getScreenshotUrl(String websiteUrl) {
                // URL 인코딩
                String encodedUrl = Uri.encode(websiteUrl);

                // 여러 스크린샷 API 옵션 중 하나 선택
                // 옵션 1: API Flash (무료 티어 제공)
                // return "https://api.apiflash.com/v1/urltoimage?access_key=YOUR_KEY&url=" + encodedUrl;

                // 옵션 2: ScreenshotAPI (무료)
                // return "https://shot.screenshotapi.net/screenshot?url=" + encodedUrl + "&width=300&height=300";

                // 옵션 3: Google PageSpeed Insights API (무료)
                // return "https://www.google.com/s2/favicons?domain=" + websiteUrl + "&sz=128";

                // 옵션 4: 간단한 파비콘 사용 (가장 안정적)
                return "https://www.google.com/s2/favicons?domain=" + extractDomain(websiteUrl) + "&sz=128";

                // 옵션 5: Screenshot Machine (무료 제한적)
                // return "https://api.screenshotmachine.com?key=YOUR_KEY&url=" + encodedUrl + "&dimension=300x300";
            }

            private String extractDomain(String url) {
                try {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                    }
                    Uri uri = Uri.parse(url);
                    return uri.getHost();
                } catch (Exception e) {
                    return url;
                }
            }

            private void incrementVisitCount(String siteId, FirebaseFirestore db) {
                db.collection("frequentSites")
                        .document(siteId)
                        .get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                Long currentCount = document.getLong("visitCount");
                                int newCount = (currentCount != null ? currentCount.intValue() : 0) + 1;

                                db.collection("frequentSites")
                                        .document(siteId)
                                        .update("visitCount", newCount);
                            }
                        });
            }
        }
    }
}