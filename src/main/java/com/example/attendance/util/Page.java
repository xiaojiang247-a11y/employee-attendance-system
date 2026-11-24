package com.example.attendance.util;

import java.util.Collections;
import java.util.List;

/** 轻量内存分页：列表数据规模在千级以内，避免引入额外分页依赖 */
public class Page<T> {
    private List<T> list = Collections.emptyList();
    private int page = 1;
    private int size = 10;
    private long total = 0;
    private int totalPages = 1;

    public static <T> Page<T> of(List<T> all, int page, int size) {
        Page<T> p = new Page<>();
        p.size = size <= 0 ? 10 : size;
        p.total = all == null ? 0 : all.size();
        p.totalPages = (int) Math.ceil((double) p.total / p.size);
        if (p.totalPages < 1) p.totalPages = 1;
        p.page = Math.max(1, Math.min(page, p.totalPages));
        if (all == null || all.isEmpty()) return p;
        int from = (p.page - 1) * p.size;
        int to = Math.min(from + p.size, all.size());
        p.list = from >= all.size() ? Collections.emptyList() : all.subList(from, to);
        return p;
    }

    public List<T> getList() { return list; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotal() { return total; }
    public int getTotalPages() { return totalPages; }
    public boolean isHasPrev() { return page > 1; }
    public boolean isHasNext() { return page < totalPages; }
}
