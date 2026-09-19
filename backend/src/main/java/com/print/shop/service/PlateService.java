package com.print.shop.service;

import com.print.shop.dto.BizException;
import com.print.shop.entity.Plate;
import com.print.shop.entity.Press;
import com.print.shop.repository.PlateRepository;
import com.print.shop.repository.PressRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlateService {

    private final PlateRepository plates;
    private final PressRepository presses;

    public PlateService(PlateRepository plates, PressRepository presses) {
        this.plates = plates;
        this.presses = presses;
    }

    public List<Plate> list(Long pressId, String state, String keyword) {
        return plates.findAllByOrderByIdAsc().stream()
                .filter(p -> pressId == null || pressId.equals(p.pressId))
                .filter(p -> state == null || state.isBlank() || state.equals(p.plateState))
                .filter(p -> keyword == null || keyword.isBlank()
                        || p.plateCode.contains(keyword) || p.plateName.contains(keyword))
                .toList();
    }

    @Transactional
    public Plate save(Plate form) {
        Plate origin = null;
        if (form.id != null) {
            origin = plates.findById(form.id).orElseThrow(() -> new BizException("印版不存在"));
            if (form.plateCode == null || form.plateCode.isBlank()) {
                form.plateCode = origin.plateCode;
            }
            if (form.plateName == null || form.plateName.isBlank()) {
                form.plateName = origin.plateName;
            }
        }
        if (form.plateCode == null || form.plateCode.isBlank()) {
            throw new BizException("印版编号不能空着");
        }
        if (form.plateName == null || form.plateName.isBlank()) {
            throw new BizException("版面名称不能空着");
        }
        form.plateCode = form.plateCode.trim();
        plates.findByPlateCode(form.plateCode).ifPresent(other -> {
            if (!other.id.equals(form.id)) {
                throw new BizException("编号 " + form.plateCode + " 已经用在别的印版上了");
            }
        });
        if (form.pressId != null) {
            Press press = presses.findById(form.pressId).orElseThrow(() -> new BizException("要装的印刷机不存在"));
            if ("封存".equals(press.pressState)) {
                throw new BizException("印刷机「" + press.pressName + "」已经封存，装不了版");
            }
        }
        if (origin == null) {
            form.plateState = form.plateState == null || form.plateState.isBlank() ? "在用" : form.plateState;
            return plates.save(form);
        }
        if (form.pressId != null) {
            origin.pressId = form.pressId;
        }
        if (form.plateSize != null && !form.plateSize.isBlank()) {
            origin.plateSize = form.plateSize;
        }
        if (form.plateDate != null) {
            origin.plateDate = form.plateDate;
        }
        if (form.plateState != null && !form.plateState.isBlank()) {
            origin.plateState = form.plateState;
        }
        origin.plateCode = form.plateCode;
        origin.plateName = form.plateName;
        return plates.save(origin);
    }
}
