import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:mallook/constants/gaps.dart';
import 'package:mallook/constants/sizes.dart';
import 'package:mallook/feature/search/api/search_api_service.dart';
import 'package:mallook/feature/search/models/hot_keyword.dart';
import 'package:mallook/feature/search/search_product_screen.dart';
import 'package:mallook/feature/search/widget/hot_keyword_grid_widget.dart';
import 'package:mallook/global/widget/custom_circular_wait_widget.dart';

class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final TextEditingController _textEditingController = TextEditingController();
  final ScrollController _appBarScrollController = ScrollController();
  final Future<List<HotKeyword>> _hotKeywords =
      SearchApiService.getHotKeywords();
  final Set<String> _searchKeywords = {};

  String _searchWord = "";

  @override
  void initState() {
    super.initState();
    _textEditingController.addListener(() {
      setState(() {
        _searchWord = _textEditingController.text;
      });
    });
  }

  void _onSearchSubmitted() {
    if (_searchWord.isNotEmpty || _searchKeywords.isNotEmpty) {
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (context) => SearchProductScreen(
            searchWord: _searchWord,
            searchKeywords: _searchKeywords,
          ),
        ),
      );
    }
  }

  void _onClearTap() {
    _textEditingController.clear();
  }

  void _onStopSearch() {
    FocusScope.of(context).unfocus();
  }

  void addSearchKeyword(String keyword) {
    setState(() {
      _appBarScrollController.animateTo(
        _appBarScrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 300),
        curve: Curves.ease,
      );

      _searchKeywords.add(keyword);
    });
  }

  void _removeSearchKeyword(String keyword) {
    setState(() {
      _searchKeywords.remove(keyword);
    });
  }

  @override
  void dispose() {
    _textEditingController.dispose();
    _appBarScrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: _onStopSearch,
      child: Scaffold(
        backgroundColor: Colors.white,
        resizeToAvoidBottomInset: false,
        appBar: AppBar(
          backgroundColor: Colors.white,
          surfaceTintColor: Colors.white,
          automaticallyImplyLeading: false,
          elevation: 1,
          shadowColor: Colors.grey.shade400,
          title: TextField(
            textInputAction: TextInputAction.search,
            onEditingComplete: _onSearchSubmitted,
            controller: _textEditingController,
            decoration: InputDecoration(
              hintText: "내 스타일 옷 검색...",
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(
                  Sizes.size8,
                ),
                borderSide: BorderSide.none,
              ),
              filled: true,
              fillColor: Colors.grey.shade200,
              contentPadding: EdgeInsets.zero,
              prefixIcon: const Row(
                mainAxisSize: MainAxisSize.min,
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  Padding(
                    padding: EdgeInsets.only(left: Sizes.size16),
                    child: FaIcon(
                      FontAwesomeIcons.magnifyingGlass,
                      color: Colors.black,
                      size: Sizes.size18,
                    ),
                  ),
                ],
              ),
              suffixIcon: Row(
                mainAxisSize: MainAxisSize.min,
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  if (_searchWord.isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(right: Sizes.size10),
                      child: GestureDetector(
                        onTap: _onClearTap,
                        child: FaIcon(
                          FontAwesomeIcons.solidCircleXmark,
                          color: Colors.grey.shade600,
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
          actions: [
            InkWell(
              borderRadius: BorderRadius.circular(
                Sizes.size20,
              ),
              onTap: _onSearchSubmitted,
              child: Text(
                '검색',
                style: TextStyle(
                  color: Theme.of(context).primaryColorDark,
                  fontWeight: FontWeight.bold,
                  fontSize: Sizes.size20,
                ),
              ),
            ),
            Gaps.h20,
          ],
        ),
        body: Column(
          children: [
            AnimatedContainer(
              height: _searchKeywords.isNotEmpty ? Sizes.size44 : 0,
              color: Theme.of(context).primaryColorLight,
              padding: const EdgeInsets.symmetric(
                vertical: Sizes.size6,
                horizontal: Sizes.size20,
              ),
              duration: const Duration(
                milliseconds: 300,
              ),
              child: ListView.separated(
                controller: _appBarScrollController,
                scrollDirection: Axis.horizontal,
                itemBuilder: (context, index) => Container(
                  padding: const EdgeInsets.symmetric(
                    vertical: Sizes.size4,
                    horizontal: Sizes.size10,
                  ),
                  decoration: BoxDecoration(
                    color: Theme.of(context).primaryColorDark,
                    borderRadius: BorderRadius.circular(
                      Sizes.size20,
                    ),
                  ),
                  child: Row(
                    children: [
                      Text(
                        '# ${_searchKeywords.elementAt(index)}',
                        style: const TextStyle(
                          color: Colors.black,
                          fontWeight: FontWeight.bold,
                          fontSize: Sizes.size14,
                        ),
                      ),
                      Gaps.h4,
                      GestureDetector(
                        onTap: () => _removeSearchKeyword(
                            _searchKeywords.elementAt(index)),
                        child: Container(
                          padding: const EdgeInsets.all(
                            Sizes.size2,
                          ),
                          decoration: BoxDecoration(
                            color: Theme.of(context).primaryColorLight,
                            borderRadius: BorderRadius.circular(
                              Sizes.size10,
                            ),
                          ),
                          child: FaIcon(
                            FontAwesomeIcons.xmark,
                            color: Colors.grey.shade700,
                            size: Sizes.size14,
                          ),
                        ),
                      )
                    ],
                  ),
                ),
                separatorBuilder: (context, index) => Gaps.h10,
                itemCount: _searchKeywords.length,
              ),
            ),
            Expanded(
              child: Container(
                padding: const EdgeInsets.symmetric(
                  vertical: Sizes.size18,
                  horizontal: Sizes.size18,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        FaIcon(
                          FontAwesomeIcons.fire,
                          color: Colors.redAccent,
                          size: Sizes.size24,
                          shadows: [
                            BoxShadow(
                              color: Colors.orange.shade300,
                              blurRadius: 5,
                              offset: const Offset(Sizes.size2, Sizes.size2),
                            )
                          ],
                        ),
                        Gaps.h12,
                        const Text(
                          '핫한 키워드',
                          style: TextStyle(
                            color: Colors.black,
                            fontSize: Sizes.size18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    Gaps.v6,
                    const Divider(),
                    FutureBuilder(
                      future: _hotKeywords,
                      builder: (context, snapshot) {
                        if (snapshot.hasData) {
                          return Expanded(
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                vertical: Sizes.size6,
                                horizontal: Sizes.size10,
                              ),
                              child: HotKeywordGridWidget(
                                hotKeywords: snapshot.data!,
                                addKeyword: addSearchKeyword,
                              ),
                            ),
                          );
                        }
                        return const Center(
                          child: CustomCircularWaitWidget(),
                        );
                      },
                    ),
                    // HotKeywordGridWidget()
                  ],
                ),
              ),
            ),
            Expanded(
              child: Container(
                padding: const EdgeInsets.symmetric(
                  vertical: Sizes.size12,
                  horizontal: Sizes.size18,
                ),
                child: const Column(
                  children: [],
                ),
              ),
            )
          ],
        ),
      ),
    );
  }
}
