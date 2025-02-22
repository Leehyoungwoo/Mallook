package io.ssafy.mallook.domain.script.application;

import io.ssafy.mallook.domain.chatgpt.dto.request.QuestionDto;
import io.ssafy.mallook.domain.chatgpt.dto.response.GptResponseDto;
import io.ssafy.mallook.domain.chatgpt.service.GptService;
import io.ssafy.mallook.domain.heart.script_heart.dao.ScriptHeartRepository;
import io.ssafy.mallook.domain.member.dao.MemberRepository;
import io.ssafy.mallook.domain.member.entity.Member;
import io.ssafy.mallook.domain.product.dao.mongo.ProductsCustomRepository;
import io.ssafy.mallook.domain.product.dao.mongo.ProductsRepository;
import io.ssafy.mallook.domain.product.dto.request.ProductHotKeywordDto;
import io.ssafy.mallook.domain.product.dto.response.ProductsPageRes;
import io.ssafy.mallook.domain.product.entity.Products;
import io.ssafy.mallook.domain.script.dao.ScriptRepository;
import io.ssafy.mallook.domain.script.dto.request.ScriptCreatDto;
import io.ssafy.mallook.domain.script.dto.request.ScriptDeleteListDto;
import io.ssafy.mallook.domain.script.dto.response.ScriptDetailDto;
import io.ssafy.mallook.domain.script.dto.response.ScriptListDto;
import io.ssafy.mallook.domain.script.dto.response.ScriptProductDto;
import io.ssafy.mallook.domain.script.entity.Script;
import io.ssafy.mallook.global.exception.BaseExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.ssafy.mallook.domain.script.dto.response.ScriptDetailDto.toDto;
import static io.ssafy.mallook.domain.script.dto.response.ScriptDetailDto.toDtoNotLogin;
import static io.ssafy.mallook.domain.script.dto.response.ScriptListDto.toDto;
import static io.ssafy.mallook.global.common.code.ErrorCode.NOT_FOUND_SCRIPT;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class ScriptServiceImpl implements ScriptService {

    private final MemberRepository memberRepository;
    private final ScriptRepository scriptRepository;
    private final ProductsRepository mongoProductsRepository;
    private final ProductsCustomRepository productsCustomRepository;
    private final ScriptHeartRepository scriptHeartRepository;
    private final GptService gptService;

    @Override
    public Long getMaxScriptId() {
        return scriptRepository.findMaxId();
    }

    @Override
    public Slice<ScriptListDto> getScriptList(Long cursor, UUID id, Pageable pageable) {
        Member proxyMember = memberRepository.getReferenceById(id);
        return scriptRepository.findByIdLessThanAndMemberOrderByIdDesc(cursor, proxyMember, pageable)
                .map(script -> {
                    String imgUrl = this.findFirstScriptImage(script);
                    return toDto(script, imgUrl);
                });
    }

    @Override
    public Slice<ScriptListDto> getScriptList(Long cursor, Pageable pageable) {
        return scriptRepository.findByIdLessThanOrderByIdDesc(cursor, pageable)
                .map(script -> {
                    String imgUrl = findFirstScriptImage(script);
                    return toDto(script, imgUrl);
                });
    }

    @Override
    public List<ScriptProductDto> getRecommendProductById(Long scriptId, Pageable pageable) {
        Script proxyScript = scriptRepository.getReferenceById(scriptId);
        List<String> scriptKeyword = proxyScript.getKeywordList();
        ProductHotKeywordDto productHotKeywordDto = ProductHotKeywordDto.builder()
                .hotKeywordList(scriptKeyword)
                .build();
        String cursor = mongoProductsRepository.findFirstByOrderById().getId().toString();

        return productsCustomRepository.findByKeywordList(productHotKeywordDto.hotKeywordList(), cursor, pageable).content()
                .stream()
                .map(ScriptProductDto::toScriptProductDto)
                .collect(toList());
    }

    @Override
    public ProductsPageRes getRecommendProductDetail(Long scriptId, String cursor, Pageable pageable) {
        Script proxyScript = scriptRepository.getReferenceById(scriptId);
        List<String> scriptKeyword = proxyScript.getKeywordList();
        ProductHotKeywordDto productHotKeywordDto = ProductHotKeywordDto.builder()
                .hotKeywordList(scriptKeyword)
                .build();

        return productsCustomRepository.findByKeywordList(productHotKeywordDto.hotKeywordList(), cursor, pageable);
    }

    public ScriptDetailDto getScriptDetail(Long scriptId) {
        return scriptRepository.findById(scriptId)
                .map(script -> {
                    String imgUrl = findFirstScriptImage(script);
                    return toDtoNotLogin(script, imgUrl);
                })
                .orElseThrow(() -> new BaseExceptionHandler(NOT_FOUND_SCRIPT));
    }

    @Override
    public ScriptListDto getLatestScript(UUID id) {
        Member proxyMember = memberRepository.getReferenceById(id);
        return scriptRepository.findTopByMemberOrderByIdDesc(proxyMember)
                .map(script -> {
                    String imgUrl = findFirstScriptImage(script);
                    return toDto(script, imgUrl);
                })
                .orElseThrow(() -> new BaseExceptionHandler(NOT_FOUND_SCRIPT));
    }

    @Override
    public ScriptDetailDto getScriptDetail(UUID memberId, Long scriptId) {
        Member proxyMember = memberRepository.getReferenceById(memberId);
        Script proxyScript = scriptRepository.getReferenceById(scriptId);
        boolean hasLike = scriptHeartRepository.findByMemberAndScript(proxyMember, proxyScript).isPresent();
        return scriptRepository.findById(scriptId)
                .map(script -> {
                    String imgUrl = findFirstScriptImage(script);
                    return toDto(script, hasLike, imgUrl);
                })
                .orElseThrow(() -> new BaseExceptionHandler(NOT_FOUND_SCRIPT));
    }

    @Override
    @Transactional
    public void createScript(ScriptCreatDto scriptCreateDto, UUID id) {
        Member proxyMember = memberRepository.getReferenceById(id);
        // ChatGPT에 질문 전달
        String scriptContent = String.join(", ", scriptCreateDto.keywordsList());
        QuestionDto questionDto = QuestionDto.builder()
                .content(scriptContent)
                .build();
        GptResponseDto gptResponseDto = gptService.askQuestion(questionDto);
        Script script = scriptCreateDto.toEntity(proxyMember, gptResponseDto.answer());
        scriptRepository.save(script);
    }

    @Override
    @Transactional
    public void deleteScript(ScriptDeleteListDto scriptDeleteListDto) {
        log.info(scriptDeleteListDto.toString());
        scriptRepository.deleteScript(scriptDeleteListDto.toDeleteList());
    }

    @Override
    public String findFirstScriptImage(Script script) {
        List<String> scriptKeyword = script.getKeywordList();
        List<Products> keywordsIn = productsCustomRepository.findByKeywordsWithLimit(scriptKeyword);
        Collections.shuffle(keywordsIn);
        return keywordsIn.get(0).getImage();
    }
}
