package com.diquest.plot.result;

import com.diquest.commons.type.ByteUtil;
import com.diquest.dqdic.transmitable.Transmitable;
import com.diquest.ejiana.core.TagSet;
import com.diquest.jiana.core.result.Additional;
import com.diquest.jiana.core.result.Eoj;
import com.diquest.jiana.core.result.Morph;
import com.diquest.jiana.result.JianaResult;
import com.diquest.ejiana.core.TagSet;
import com.diquest.jiana3.hdic.DQTagDic;
import com.diquest.plot.PLOT;
import com.diquest.plot.PatternEntry;
import com.diquest.plot.prop.PLOTLanguage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * PLO 결과 객체. 분석한 결과들을 담고있다.
 * 
 * @author 박준형
 * 
 * */
public abstract class PLOTResult extends Transmitable {
	
	protected JianaResult jianaResult = new JianaResult();
	
	/** 기본으로 늘려줄 배열 크기 기준. */
	protected final static int DEFAULT_SIZE=1024;

	/** 총 추출한 Named Entity 개수. */
	protected int ne_total = 0;

	/** 패턴 배열. */
	protected String[] pattern;

	/** 패턴 위치 배열. */
	protected int[] pattern_pos;
	
	/** 패턴 크기 배열. */
	protected int[] pattern_size;
	
	/** 패턴 범위 배열. */
	protected int[] pattern_range;

	/** 정규 표현 개체명 시작 위치 배열 */
	protected int[] pattern_regex_pos;

	/** 정규 표현 개체명 끝 위치 배열*/
	protected int[] pattern_regex_end;

	/** 렉시컬 배열. */
	protected String[] lexicalArray;
	
	/** 개체명 배열. */
	protected String[] ne_res;

	/** 개체명 원본 배열. */
	protected String[] ne_resOrigin;
	
	int[] ne_type;
	/** 어절 정보를 넘겨 주기 위해서 05/08/11 */
	protected int[] eoj_res;

	protected String[] lex;

	protected String[] lexOrigin;
	
	protected String[] ploTag;
	
	protected String[] mTag;
	
	protected int[] groupNum;

	protected int[] range;

	/** 오타 보정된 문장 **/
	protected String correctTypingSentence;
	
	/** 제외된 개체 정보 */
    protected ArrayList<String> excludedNEs = new ArrayList<String>();
    protected ArrayList<String> excludedNETypes = new ArrayList<String>();
    protected ArrayList<String> exclusionPatternsUsed = new ArrayList<String>();    

    /** 정규식으로 뽑힌 개체명 시작 위치 **/
    protected  ArrayList<Integer> regexPos = new ArrayList<>();

    /** 정규식으로 뽑힌 개체명 끝 위치 */
    protected  ArrayList<Integer> regexEnd = new ArrayList<>();

	public String getCorrectTypingSentence(){
		return correctTypingSentence;
	}

	protected String regexpattern;
	/**
	 * 배열들에 메모리를 넘겨받은 파라메터 크기 기준으로 할당해준다.
	 * 
	 * @param	size : 메모리를 확장시켜줄 크기.
	 * 
	 * */
	protected void assign(int size) {

		pattern = new String[size*4];
		pattern_pos = new int[size*4];
		pattern_size = new int[size*4];
		pattern_range = new int[size*4];
		pattern_regex_pos = new int[size*4];
		pattern_regex_end = new int[size*4];

		ne_res = new String[size];
		ne_resOrigin = new String[size];
		eoj_res = new int[size];
		groupNum = new int[size];
	}

	/**
	 * PLO 결과 객체를 초기화 한다. 단, 초기화 하는 것은 저장한
     * NE의 개수와 제외된 개체 정보 뿐이다. 
     * 나머지 배열들은 재활용 한다.
	 * 
	 * */
	public void clear() {
		this.ne_total = 0;
		assign(DEFAULT_SIZE);
        this.excludedNEs.clear();
        this.excludedNETypes.clear();
        this.exclusionPatternsUsed.clear();
	}

	public void setNE(String ne, String group, PatternEntry.Item[] item, int eoj, int[] range){
		setNE(ne, group, item, eoj, range, 99999);
	}

	/**
	 * 추출한 개체명을 결과 객체에 기록한다.
	 *
	 * @param	ne    : 개체명 이름.
	 * @param	group : 그룹, 카테고리명.
	 * @param	item  : 패턴 아이템.
	 * @param	eoj   : 어절 끝 위치.
	 *
	 * */
	public void setNE(String ne, String group, PatternEntry.Item[] item, int eoj, int[] range, int groupNum) {
		if (pattern == null || pattern.length == 0) {
			assign(DEFAULT_SIZE);
		}

		/** 메모리 공간이 부족하면 증가시켜준다. */
		if (ne_total == ne_res.length) {
			increaseSize();
		}

		ne_res[ne_total] = ne;
		ne_resOrigin[ne_total] = ne;
		eoj_res[ne_total] = eoj;
		pattern[ne_total * 4] = group;
		this.groupNum[ne_total] = groupNum;
		for(int i=0;i<3;i++) {
			int index=(ne_total*4)+1+i;
			
			pattern[index]=item[i].pattern;
			pattern_pos[index]=item[i].pos;
			pattern_size[index]=item[i].size;
			pattern_regex_pos[index] = item[i].regex_pos;
			pattern_regex_end[index] = item[i].regex_end;
			pattern_range[index]=range[i];
		}

		ne_total++;
	}

	/**
	 * matchProcess의 addResultOrder를 통해 어휘 패턴 제거를 위해 사용하는 메소드
	 * @param set 제거할 어휘 패턴 개체명 index를 가지는 set
	 */
	public void reSetNE(HashSet<Integer> set){

		//개체명 추출과 관련된 전역 변수들 임시 변수에 저장
		String[] tempNeRes = ne_res;
		String[] tempNeResOrigin = ne_resOrigin;
		int[] tempEojRes = eoj_res;
		String[] tempPattern = pattern;
		int[] tempGroupNum = groupNum;
		int[] tempPatternPos = pattern_pos;
		int[] tempPatternSize = pattern_size;
		int[] tempPatternRegexPos = pattern_regex_pos;
		int[] tempPatternRegexEnd = pattern_regex_end;
		int[] tempPatternRange = pattern_range;
		int beforeNeNum = ne_total;

		ne_total = 0; //전체 개체명 개수를 나타내는 변수 0으로 초기화
		assign(DEFAULT_SIZE); //개체명 추출에 사용하는 전역 변수 초기화

		for(int i = 0; i<beforeNeNum; i++){ //어휘 패턴 제거전 개체명 개수 만큼 루프
			if(set.contains(i)){ //입력 set에 해당 인덱스가 있을 경우 continue
				continue;
			}

			//개체명 추출에 사용되는 전역 변수에 임시 변수들 할당
			ne_res[ne_total] = tempNeRes[i];
			ne_resOrigin[ne_total] = tempNeResOrigin[i];
			eoj_res[ne_total] = tempEojRes[i];
			pattern[ne_total * 4] = tempPattern[i*4];
			groupNum[ne_total] = tempGroupNum[i];
			for(int j=0;j<3;j++){
				int tempIndex = (i*4)+1+j;
				int neIndex = (ne_total*4)+1+j;

				pattern[neIndex] = tempPattern[tempIndex];
				pattern_pos[neIndex] = tempPatternPos[tempIndex];
				pattern_size[neIndex] = tempPatternSize[tempIndex];
				pattern_regex_pos[neIndex] = tempPatternRegexPos[tempIndex];
				pattern_regex_end[neIndex] = tempPatternRegexEnd[tempIndex];
				pattern_range[neIndex] = tempPatternRange[tempIndex];
			}
			ne_total++;
		}

	}

	public void sortNE() {
		String[] temp_ne_res = new String[getNENum()];
		String[] temp_ne_origin = new String[getNENum()];
		int[] temp_eoj_res = new int[getNENum()];
		String[] temp_pattern = new String[getNENum() * 4];
		int[] temp_pattern_pos = new int[getNENum() * 4];
		int[] temp_pattern_size = new int[getNENum() * 4];
		int[] temp_pattern_range = new int[getNENum() * 4];
		ArrayList<Integer> pos_list = posListAssign();
		ArrayList<Integer> pos_sort_list = posListAssign();
		Collections.sort(pos_sort_list);

		for (int i = 0; i < ne_total; i++) {
			int index = pos_list.indexOf(pos_sort_list.get(i));
			pos_list.set(index, -1);  // 이미 사용한 요소는 제거

			temp_ne_origin[i] = ne_resOrigin[index];
			temp_ne_res[i] = ne_res[index];
			temp_eoj_res[i] = eoj_res[index];
			temp_pattern[i * 4] = pattern[index * 4];

			for (int j = 0; j < 3; j++) {
				int p_index = (i * 4) + 1 + j;
				int sort_p_index = (index * 4) + 1 + j;
				temp_pattern[p_index] = pattern[sort_p_index];
				temp_pattern_pos[p_index] = pattern_pos[sort_p_index];
				temp_pattern_size[p_index] = pattern_size[sort_p_index];
				temp_pattern_range[p_index] = pattern_range[sort_p_index];
			}
		}

		ne_res = temp_ne_res;
		ne_resOrigin = temp_ne_origin;
		eoj_res = temp_eoj_res;
		pattern = temp_pattern;
		pattern_pos = temp_pattern_pos;
		pattern_size = temp_pattern_size;
		pattern_range = temp_pattern_range;
	}


	private ArrayList<Integer> posListAssign() {
		ArrayList<Integer> temp = new ArrayList<>();
		for (int i = 0; i < getNENum(); i++) {
			temp.add(getCenterPatternPos(i));
		}

		return temp;
	}

	private static final String INFOCHATTER_PAT = "@term_in_infochatter";
	public void setInfochatterNE(String ne, String neTag){
		ntPatItem[1].pattern = INFOCHATTER_PAT;
		ntPatItem[1].pos = 0;
		ntPatItem[1].size = 0;
		
		int eojIdx = 0;
		int[] range = new int[3];

		range[0] = 0;
		range[1] = 0;
		range[2] = 0;

		setNE(ne, neTag, ntPatItem, eojIdx, range);
	}
	
//	/**
//	 * 추출한 개체명을 결과 객체에 기록한다.
//	 * 
//	 * @param	ne    : 개체명 이름.
//	 * @param	group : 그룹, 카테고리명.
//	 * @param	item  : 패턴 아이템.
//	 * @param	eoj   : 어절 끝 위치.
//	 * 
//	 * */
//	public abstract void setNE(String ne, String group, PatternEntry.Item[] item, int eoj, int[] range);
//	
	
	private PatternEntry.Item[] ntPatItem = new PatternEntry.Item[3];;
	PatternEntry ntPatEnt = new PatternEntry();
	
	/**
	 * PLO 결과 객체 초기화. 렉시컬 배열값을 받아온다.
	 * 
	 * @param	lexical : 렉시컬 배열을 초기화해준다.
	 * 
	 * */
	public void init(String[] lexical) {
		this.lexicalArray=lexical;
		for(int i=0 ; i<3 ; i++)
			ntPatItem[i] = ntPatEnt.getItem();
	}
	
	public void setNE(String str, int i){
		this.ne_res[i] = str;
	}
	
	public void setNEOrigin(String str, int i){
		if (str==null){
			this.ne_resOrigin[i] = "";
		}
		else{
			this.ne_resOrigin[i] = str;
		}
	}
	
	/**
	 * 추출한 Named Entity를 스트링 배열로 반환한다.
	 * 객체를 아예 새로 만들어서 반환.
	 * 
	 * @return	추출한 Named Entity 스트링 배열
	 * 
	 * */
	public String[] getNE(){
		if(ne_total == 0) 
			return null;
		String[] temp_ne = new String[ne_total];
		System.arraycopy(ne_res,0,temp_ne,0,ne_total);
		return temp_ne;		
	}
	
	public String[] getNEOrigin(){
		if(ne_total == 0) 
			return null;
		String[] temp_ne = new String[ne_total];
		System.arraycopy(ne_resOrigin,0,temp_ne,0,ne_total);
		return temp_ne;		
	}
	
	public String getNE(int i) {
		if (ne_total == 0 || i >= ne_total || i < 0) {
			return null;
		}

		return ne_res[i];
	}
	
	public String getNEOrigin(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return null;
		return ne_resOrigin[i];
	}
	
	public String getNETag(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return null;
		return pattern[i*4];
	}
	
	/**
	 * 렉시컬 배열을 반환한다.
	 * 객체를 아예 새로 만들어서 반환.
	 * 
	 * @return	반환할 렉시컬 배열.
	 * 
	 * */
	public String[] getLexical(){
		if(lexicalArray.length == 0) 
			return null;
		String[] temp_lex = new String[lexicalArray.length];
		System.arraycopy(lexicalArray,0,temp_lex,0,lexicalArray.length);
		return temp_lex;		
	}
	
	/**
	 * 어절 끝 정보를 반환한다.
	 * 
	 * @return	어절 끝 정보.
	 * 
	 * */
	public int[] getEoj(){
		if(ne_total == 0) 
			return null;		
		return eoj_res;		
	}
	
	public int getEoj(int i){
		if(ne_total == 0) 
			return -1;		
		return eoj_res[i];		
	}
	
	/**
	 * 추출한 개체명의 개수를 반환한다.
	 * 
	 * @return	추출한 개체명의 개수.
	 * 
	 * */
	public int getNENum() {
		return this.ne_total;
	}
	
	public String[] getPattern(){
		return this.pattern;
	}
	public int[] getPatternPos(){
		return this.pattern_pos;
	}
	public int[] getPatternSize(){
		return this.pattern_size;
	}
	public int[] getPatternRange(){
		return this.pattern_range;
	}
	public int[] getPatternRange(int neIndex) {
		int[] range = new int[3];
		int startIndex = neIndex*4;
		
		range[0] = pattern_range[startIndex+1];
		range[1] = pattern_range[startIndex+2];
		range[2] = pattern_range[startIndex+3];
		
		return range;
	}
	
	public String[] getPloTagArray() {
		return this.ploTag;		
	}
	public String getPloTag(int i) {

		try{
			if(i >= getPloTagArray().length || i < 0)
				return null;
			return this.ploTag[i];
		}catch(NullPointerException e){
			e.printStackTrace();
			return null;
		}

	}
	public String[] getLexicalArray() {
		return this.lex;
	}
	public String[] getLexicalOriginArray() {
		return this.lexOrigin;
	}
	public String getLexical(int i){

		//null 처리
		try {
			if (i >= getLexicalArray().length || i < 0)
				return null;
			return this.lex[i];
		}catch(NullPointerException e){
			e.printStackTrace();
			return null;
		}
	}
	public String getLexicalOrigin(int i){

		try{
			if(i >= getLexicalArray().length || i < 0)
				return null;
			return this.lexOrigin[i];
		}catch(NullPointerException e){
			e.printStackTrace();
			return null;
		}


	}
	public String[] getMorTagArray() {
		return this.mTag;
	}
	public String getMorTag(int i) {
		//null 처리

		try{
			if(i >= getMorTagArray().length || i < 0)
				return null;
			return this.mTag[i];
		}catch(NullPointerException e){
			e.printStackTrace();
			return null;
		}

	}	
	public int[] getRangeArray() {
		return this.range;
	}

	public void setregexPattern(String pattern) {
		regexpattern = pattern;
	}
	public void setRangeArray(int[] range) {
		this.range = range;
	}
	
	public void setPloTagArray(String[] ploTag) {
		this.ploTag = ploTag;		
	}
	public void setLexicalArray(String[] lex) {
		this.lex = lex;
	}
	public void setLexicalArrayOrigin(String[] lexOrigin) {
		this.lexOrigin = lexOrigin;
	}
	public void setMorTagArray(String[] mTag) {
		this.mTag = mTag;
	}
	
	/** 언어별 PLOT result 별로 사용할 JianaResult 내의 자원을 설정하는 함수 */
	abstract protected void setJianaNeeds();
	public void setJianaResult(JianaResult result){
		this.jianaResult = result;
		setJianaNeeds();
		
		correctTypingSentence = "";
		for(int i=0; i<this.jianaResult.getEoj().getCount(); i++){
			correctTypingSentence += new String(this.jianaResult.getEoj().getData(i));
			
			if(i < this.jianaResult.getEoj().getCount()-1){
				correctTypingSentence += " ";
			}
		}
		/*System.out.println("PLOTResult.init() correctTypingSentence : " + correctTypingSentence);*/
	}
	
	public JianaResult getJianaResult(){
		return this.jianaResult;
	}
	
	/**
	 * 주어진 인덱스 위치의 패턴의 시작 어절 위치를 반환한다.
	 * 
	 * @param	index : 패턴의 시작 형태소 인덱스.
	 * @return	      : 해당 패턴의 어절 위치 인덱스.
	 */
	public abstract int getPatEojStart(int index);
	
	/**
	 * 주어진 인덱스 위치의 패턴의 끝 어절 위치를 반환한다.
	 * 
	 * @param	index : 패턴의 끝 형태소 인덱스.
	 * @return	      : 해당 패턴의 어절 위치 인덱스.
	 */
	public abstract int getPatEojEnd(int index);

	/**
	 * 디버그용. 입력 스트림에 넘겨진 객체의 값을 출력한다.
	 * 
	 * @param	result : 출력할 결과 객체.
	 * @param	st     : 객체를 출력할 스트림.
	 * 
	 */
	public static void debug(PLOTResult result, PrintStream st) {
		
		String[] ne=result.getNE();
		
		if(ne==null)
			return;
		
		for(int i=0,j=result.ne_total;i<j;i++) {
			st.print(ne[i]);
		}	
	}
	
	/**
	 * 디버그용. 객체의 정보를 스트링화한다.
	 * PLO 결과 객체를 그대로 프린트해주는 메소드.
	 * 프린트할 내용을 스트링에 담아서 반환한다.
	 * 
	 * @param	result : 출력할 결과객체
	 * @return	       : 스트링으로된 출력 내용.
	 * 
	 */
	public static String debug(PLOTResult result) {		
		if (result==null)
			return null;
		
		String[] lex=result.lexicalArray;
		int[] eoj=result.eoj_res;
		String[] ne=result.getNE();
		
		String[] pattern=result.pattern;
		int[] pattern_pos=result.pattern_pos;
		int[] pattern_size=result.pattern_size;
		int[] pattern_range=result.pattern_range;
		
		
		StringBuffer sb=new StringBuffer();
		
		sb.append("lexical: \t");
		if (ne!=null) {
			for(int i=0,j=lex.length;i<j;i++) {
				sb.append(lex[i]).append(" ");
			}			
		}
		
		sb.append("\r\neoj resource: \t");
		if (eoj!=null) {
			for(int i=0,j=result.ne_total-1;i<j;i++) {
				sb.append("[").append(eoj[i]).append("], ");
			}			
			if (result.ne_total>0) {
				sb.append("[").append(eoj[result.ne_total - 1]).append("]");
			}
		}
		
		sb.append("\r\nnamed entity: \t");
		if (ne!=null) {
			for(int i=0,j=result.ne_total-1;i<j;i++) {
				sb.append("[").append(ne[i]).append("], ");
			}	
			if (result.ne_total>0) {
				sb.append("[").append(ne[result.ne_total - 1]).append("]");
			}
		}
		
		int size=result.ne_total*4;
		
		sb.append("\r\npattern: \t");
		if (pattern!=null) {
			for(int i=0,j=size-1;i<j;i++) {
				if (pattern[i]==null)
					break;
				sb.append("[").append(pattern[i]).append("], ");
			}	
			if (size>0) {
				if (pattern[size-1]!=null) {
					sb.append("[").append(pattern[size - 1]).append("]");
				}
			}
		}
		
		sb.append("\r\npattern pos: \t");
		if (pattern_pos!=null) {
			for(int i=0,j=size-1;i<j;i++) {
				sb.append("["+pattern_pos[i]+"], ");
			}			
			if (size>0) {
				sb.append("["+pattern_pos[size-1]+"], ");
			}
		}
		
		sb.append("\r\npattern size: \t");
		if (pattern_size!=null) {
			for(int i=0,j=size-1;i<j;i++) {
				sb.append("[").append(pattern_size[i]).append("], ");
			}			
			if (size>0) {
				sb.append("[").append(pattern_size[size - 1]).append("]");
			}
		}
		
		sb.append("\r\npattern range: \t");
		if (pattern_range!=null) {
			for(int i=0,j=size-1;i<j;i++) {
				sb.append("[").append(pattern_range[i]).append("], ");
			}			
			if (size>0) {
				sb.append("[").append(pattern_range[size - 1]).append("]");
			}
		}
		sb.append("\r\n");
		
		return sb.toString();
		
	}
	
	public String getNounSequence(){
		StringBuffer sb = new StringBuffer();
		
		for(int i = 0; i< getLexicalArray().length; i++){
			for(String nounTag: PLOT.getNounGroupSet()){
				if(getMorTag(i).equals(nounTag)){
					sb.append(getLexical(i)).append(" ");
					break;
				}
			}
		}
		
		return sb.toString().trim();
	}
		
	String[] resultSequence;
	
	public String getResultSequence() {
		StringBuffer sb = new StringBuffer();
		resultSequence = new String[getLexicalArray().length];

		for (int i = 0; i < getLexicalArray().length; i++) {
			if (getPloTag(i) == null) {
				resultSequence[i] = getLexical(i) + "/@" + getMorTag(i);
			}
			else {
				resultSequence[i] = getLexical(i) + "/" + getPloTag(i);
			}
		}

		String curNEStr = null;
		String prevNEStr = "";
		int startPos = -1;
		int size = -1;
		int prevEojIdx = -1;

		for (int i = 0; i < getNENum(); i++) {
			curNEStr = getNE(i);
			if (prevNEStr.equals(curNEStr)) {
				if (prevEojIdx == getEoj(i)) {
					continue;
				}
			}

			if (getCenterPatternPos(i) >= 0) {
				startPos = getCenterPatternPos(i);
				size = getCenterPatternSize(i);

				resultSequence[startPos] = getNE(i) + "/" + getPattern(i);
				for (int j = startPos + 1; j < startPos + size; j++) {
					resultSequence[j] = "";
				}
			}

			prevNEStr = curNEStr;
			prevEojIdx = getEoj(i);
		}

		for (String str : resultSequence) {
			if (resultSequence.equals(""))
				continue;
			sb.append(str).append(" ");
		}

		return sb.toString().trim();
	}
	
	
	ArrayList<String> bratStrList = null;
	
	ArrayList<BratResult> plotBratList = null;
	ArrayList<BratResult> jianaBratList = null;
	public ArrayList<String> getEntityBratList(int revise){
		bratStrList = new ArrayList<String>();
		
		StringBuffer sb = new StringBuffer();
		
		int count = 0;
		
		plotBratList = new ArrayList<BratResult>();
		
		jianaBratList = new ArrayList<BratResult>();
		
		Eoj eoj = this.jianaResult.getEoj();
		Morph morph = this.jianaResult.getMorph();

		//각 어절마다
		for (int i = 0; i < this.jianaResult.getEoj().getCount(); i++) {
			short begMorph = (short) ((i > 0) ? eoj.getMorphOfEojEnd(i-1)+1 : 0);
			short endMorph = (short) eoj.getMorphOfEojEnd(i);
			sb = new StringBuffer();
			sb.append("M: ");
			for (short  j= begMorph; j <= endMorph; j++){
				String addStr = new String(morph.getData(j));
				if(addStr.contains("\""))
					addStr = addStr.replace("\"", "\\\"");
				sb.append(addStr).append("/");
				//sb.append(TagSet.tagStrings[this.jianaResult.getTag().getTag(j, 0)]);
				//DQTagDic.tag.main.getName(jianaResult.getTag().getTag(k))
				sb.append(DQTagDic.tag.main.getName(jianaResult.getTag().getTag(j)));
				if (j != endMorph){
					sb.append("+");
				}
			}
			jianaBratList.add(new BratResult(sb.toString(), morph.getMorphPosBySyllable(begMorph), 
					(morph.getMorphPosBySyllable(begMorph)+eoj.getData(i).length)));			
		}
		
		int startIdx;
		int endIdx;
		
		String prevStr ="";
		int prevEojIdx = -1;
		
		for(int i=0; i<ne_total; i++){
			
			// 우선순위가 가장 높은 결과만 사용
			if(prevStr.equals(ne_res[i])){
				if(prevEojIdx == eoj_res[i])
					continue;
			}
			
			startIdx = -1;
			endIdx = -1;
			sb = new StringBuffer();
			sb.append("N: ").append(ne_res[i]).append("/").append(pattern[i * 4]);
			
			for(BratResult br : jianaBratList){
				//어절 전체로 위치 보정
				if(startIdx==-1){
					startIdx = br.startCheck(pattern_range[i*4+1]);					
				}
				if(startIdx!=-1){
					endIdx = br.endCheck(pattern_range[i*4+2]);
					if(endIdx!=-1)
						break;
				}				
			}
			if(endIdx == -1)
				endIdx = jianaBratList.get(jianaBratList.size()-1).end;
			plotBratList.add(new BratResult(sb.toString(), startIdx, endIdx));
			
			prevStr = ne_res[i];
			prevEojIdx = eoj_res[i];
		}		
		
		for(BratResult br : jianaBratList){
			count++;
			bratStrList.add(br.toString(revise));
		}
		for(BratResult br : plotBratList){
			count++;
			bratStrList.add(br.toString(revise));
		}
		
		return bratStrList;
	}
	
	public String toStringNeList(){
		StringBuffer sb = new StringBuffer();
			
		for(int i = 0 ; i < getNENum(); i++){
			if(i!=0)
				sb.append(',');
			sb.append(getNETag(i)).append('=').append(getNE(i));
//			sb.append(getNE(i)).append('/').append(getNETag(i));
		}
		
		return sb.toString();
	}
	
	public String toStringMeanList(){
		StringBuffer sb = new StringBuffer();
			
		boolean flag = false;
		for(int i = 0; i < getLexicalArray().length; i++) {
			if(getPloTag(i)!=null){
				if(!flag)
					flag = true;
				else
					sb.append(',');
				sb.append(getLexical(i)).append('/').append(getPloTag(i));
			}
		}
		
		return sb.toString();
	}
	
	public String toStringLexicalArray(){
		StringBuffer sb=new StringBuffer();
		sb.append("Lexical Array:\n");
		for (int i = 0; i < getLexicalArray().length; i++) {
			sb.append(getLexical(i)).append("[").append(getMorTag(i)).append(",").append(getPloTag(i)).append(",").append(Integer.toString(i)).append("]/");
			//sb.append(getLexical(i)).append("[").append(getMorTag(i)).append(",").append(getPloTag(i)).append("]/");
		}
		return sb.toString() ;
	}

	public String toStringExtractResult(){
		StringBuffer sb=new StringBuffer();

		sb.append("Extract Result:\n");
		for(int i = 0; i < getNENum(); i++) {

			sb.append("regexPattern: " + regexpattern);
			sb.append(getNE(i)).append('[').append(getPattern(i)).append(']').append('\n');
			sb.append("eojIndex = ").append(getEoj(i)).append("\n");
			if(getLeftPatternPos(i) >= 0)
				sb.append("leftPattern = ").append(getLeftPattern(i)).append("(").append(getLeftPatternPos(i)).append(" ~ ").append(getLeftPatternPos(i) + getLeftPatternSize(i) - 1).append(")\n");
			if(getCenterPatternPos(i) >= 0)
				sb.append("centerPattern = ").append(getCenterPattern(i)).append("(").append(getCenterPatternPos(i)).append(" ~ ").append(getCenterPatternPos(i) + getCenterPatternSize(i) - 1).append(")\n");
			if(getRightPatternPos(i) >= 0)
				sb.append("rightPattern = ").append(getRightPattern(i)).append("(").append(getRightPatternPos(i)).append(" ~ ").append(getRightPatternPos(i) + getRightPatternSize(i) - 1).append(")\n");
			sb.append("input word = ").append(getNEOrigin(i)).append('\n');
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String toStringExcludedNEInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append("Excluded Named Entities:\n");
		for(int i = 0; i < this.excludedNEs.size(); i++) {
			sb.append("NE: ").append(this.excludedNEs.get(i)).append('\n');
			sb.append("NE Type: ").append(this.excludedNETypes.get(i)).append('\n');
			sb.append("Pattern Used: ").append(this.exclusionPatternsUsed.get(i)).append("\n\n");
		}
		return sb.toString();
	}
	
	public void addResult(String ne, String neTag){
		
	}

	public abstract PLOTLanguage getPlotLanguage();

	public String toString() {
		StringBuffer sb=new StringBuffer();

		sb.append(toStringLexicalArray());
		sb.append("\n\n");
		sb.append(toStringExtractResult());
		sb.append("\n");
		sb.append(toStringExcludedNEInfo());
		sb.append("\n");
		sb.append("오타 보정 결과 : ").append(correctTypingSentence);
		sb.append("\n");
		
		return sb.toString();
	}

	
	/**

     * @param s

     * @return

     */

    private int count(String s) {
          if (s==null || s.length()==0)
                 return -1;
          int count=0;
          for(int i=0,j=s.length();i<j;i++) {
                 char c=s.charAt(i);
                 if (c=='@' || c=='%' || c=='#')
                        count++;
//                 else if (c=='@')
//                        count++;
          }
          if (count==0)
                 return -1;
          return count;
    }
	
	public String getPattern(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return null;
		return pattern[i*4];
	}
	
	public String getLeftPattern(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return null;
		return pattern[i*4+1];
	}
	
	public String getCenterPattern(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return null;
		return pattern[i*4+2];
	}
	
	public String getRightPattern(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return null;
		return pattern[i*4+3];
	}
	
	public int getLeftPatternPos(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return -1;
		return pattern_pos[i*4+1];
	}
	
	public int getCenterPatternPos(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return -1;
		return pattern_pos[i*4+2];
	}
	public void setCenterPatternPos(int i, int pos){
    	if(ne_total==0 || i >= ne_total || i<0)
    		return;
		pattern_pos[i*4+2] = pos;
	}
	
	public int getRightPatternPos(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return -1;
		return pattern_pos[i*4+3];
	}
	
	public int getPatternPos(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return -1;
		return pattern_pos[i*4];
	}
	
	public int getLeftPatternSize(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return -1;
		return pattern_size[i*4+1];
	}
	
	public int getCenterPatternSize(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return -1;
		return pattern_size[i*4+2];
	}

	public void setCenterPatternSize(int i, int size){
		if(ne_total==0 || i >= ne_total || i<0)
			return;
		pattern_size[i*4+2]=size;
	}

	public int getRightPatternSize(int i){
		if(ne_total==0 || i >= ne_total || i<0)
			return -1;
		return pattern_size[i*4+3];
	}
	

	
	/**
	 * 배열들의 크기를 늘려주기 위한 메소드.
	 * 기준 크기보다 큰 공간이 필요하면 증가하고 감소하지는 않는다.
	 * 
	 * */
	private void increaseSize() {
		
		String[] tempStr = new String[pattern.length * 2];
		System.arraycopy(pattern,0,tempStr,0,pattern.length);
		pattern = tempStr;
		
		tempStr = new String[ne_res.length * 2];
		System.arraycopy(ne_res,0,tempStr,0,ne_res.length);
		ne_res = tempStr;
		
		tempStr = new String[ne_resOrigin.length * 2];
		System.arraycopy(ne_resOrigin,0,tempStr,0,ne_resOrigin.length);
		ne_resOrigin = tempStr;
								
		int[] tempInt = new int[pattern_pos.length * 2];
		System.arraycopy(pattern_pos,0,tempInt,0,pattern_pos.length);
		pattern_pos = tempInt;
		
		tempInt = new int[pattern_size.length * 2];
		System.arraycopy(pattern_size,0,tempInt,0,pattern_size.length);
		pattern_size = tempInt;
		
		tempInt = new int[pattern_range.length * 2];
		System.arraycopy(pattern_range,0,tempInt,0,pattern_range.length);
		pattern_range = tempInt;
		
		tempInt = new int[eoj_res.length * 2];
		System.arraycopy(eoj_res,0,tempInt,0,eoj_res.length);
		eoj_res = tempInt;
		
		tempInt = new int[groupNum.length * 2];
		System.arraycopy(groupNum,0,tempInt,0,groupNum.length);
		groupNum = tempInt;

		tempInt = new int[pattern_regex_pos.length*2];
		System.arraycopy(pattern_regex_pos, 0, tempInt, 0, pattern_regex_pos.length);
		pattern_regex_pos = tempInt;

		tempInt = new int[pattern_regex_end.length*2];
		System.arraycopy(pattern_regex_end, 0, tempInt, 0, pattern_regex_end.length);
		pattern_regex_end = tempInt;

		tempInt = null;
		tempStr = null;
	}

	public void addRegPos(int pos){
		this.regexPos.add(pos);
	}

	public void addRegEnd(int end){
		this.regexEnd.add(end);
	}

	public ArrayList<Integer> getRegPos(){
		return this.regexPos;
	}

	public ArrayList<Integer> getRegEnd() { return this.regexEnd; }

	public void addExcludedNEInfo(String NE, String NEType, String centerPattern, PatternEntry excPattern) {
		excludedNEs.add(NE);
		excludedNETypes.add(NEType);
		exclusionPatternsUsed.add(excPattern.left.pattern + '|' + centerPattern + '|' + excPattern.right.pattern);	
	}
	
	public ArrayList<String> getExcludedNEs() {
		return excludedNEs;
	}
	
	public ArrayList<String> getExcludedNETypes() {
		return excludedNETypes;
	}
	
	public ArrayList<String> getExclusionPatternsUsed() {
		return exclusionPatternsUsed;
	}

	class BratResult extends Transmitable{
		String tag;
		int start;
		int end;
		
		public BratResult(String tag, int start, int end){
			this.tag = tag;
			this.start = start;
			this.end = end;
		}

		public BratResult() {
		}

		public int startCheck(int idx){
			if(start<=idx&&idx<=end)
				return start;
			return -1;
		}
		
		public int endCheck(int idx){
			if(start<=idx&&idx<=end)
				return end;
			return -1;
		}
		
		public String toString(){
			return "\""+tag+"\"', "+"[["+start+", "+end+"]]";	
		}
		
		public String toString(int revise){
			return "\""+tag+"\", "+"[["+(start+revise)+", "+(end+revise)+"]]";
		}

		@Override
		public boolean isNull() {
			return false;
		}

		@Override
		public void serialize(OutputStream outputStream) throws IOException {
			ByteUtil.writeString(outputStream, tag);
			ByteUtil.writeInt(outputStream, start);
			ByteUtil.writeInt(outputStream, end);
		}

		@Override
		public void deserialize(InputStream inputStream) throws IOException {
			tag = ByteUtil.readString(inputStream);
			start = ByteUtil.readInt(inputStream);
			end = ByteUtil.readInt(inputStream);
		}

		@Override
		public Transmitable getInstance() {
			return new BratResult();
		}
	}

	@Override
	public void serialize(OutputStream outputStream) throws IOException {
		jianaResult.serialize(outputStream);
		//추출된 NE 개수
		ByteUtil.writeInt(outputStream, ne_total);
		//패턴배열
		if(pattern != null) {
			ByteUtil.writeInt(outputStream, pattern.length);
			for (String s : pattern) {
				ByteUtil.writeString(outputStream, s);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}

		//패턴 위치배열
		if(pattern_pos != null) {
			ByteUtil.writeInt(outputStream, pattern_pos.length);
			for (int i : pattern_pos) {
				ByteUtil.writeInt(outputStream, i);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}

		//패턴 크기배열
		if(pattern_size != null) {
			ByteUtil.writeInt(outputStream, pattern_size.length);
			for (int i : pattern_size) {
				ByteUtil.writeInt(outputStream, i);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}
		//패턴 범위 배열
		if(pattern_range != null) {
			ByteUtil.writeInt(outputStream, pattern_range.length);
			for (int i : pattern_range) {
				ByteUtil.writeInt(outputStream, i);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}
		//렉시컬 배열
		if(lexicalArray != null) {
			ByteUtil.writeInt(outputStream, lexicalArray.length);
			for (String s : lexicalArray) {
				ByteUtil.writeString(outputStream, s);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}
		//개체명 배열
		if(ne_res != null) {
			ByteUtil.writeInt(outputStream, ne_res.length);
			for (String s : ne_res) {
				ByteUtil.writeString(outputStream, s);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}
		//개체명 배열
		if(ne_resOrigin != null) {
			ByteUtil.writeInt(outputStream, ne_resOrigin.length);
			for (String s : ne_resOrigin) {
				ByteUtil.writeString(outputStream, s);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}
		//어절 정보
		if(eoj_res != null) {
			ByteUtil.writeInt(outputStream, eoj_res.length);
			for (int i : eoj_res) {
				ByteUtil.writeInt(outputStream, i);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}

		if(lex != null) {
			ByteUtil.writeInt(outputStream, lex.length);
			for (String s : lex) {
				ByteUtil.writeString(outputStream, s);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}

		if(ploTag != null) {
			ByteUtil.writeInt(outputStream, ploTag.length);
			for (String s : ploTag) {
				ByteUtil.writeString(outputStream, s);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}

		if(mTag != null) {
			ByteUtil.writeInt(outputStream, mTag.length);
			for (String s : mTag) {
				ByteUtil.writeString(outputStream, s);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}

		if(groupNum != null) {
			ByteUtil.writeInt(outputStream, groupNum.length);
			for (int i : groupNum) {
				ByteUtil.writeInt(outputStream, i);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}
		if(range != null) {
			ByteUtil.writeInt(outputStream, range.length);
			for (int i : range) {
				ByteUtil.writeInt(outputStream, i);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}
		ByteUtil.writeString(outputStream, correctTypingSentence);
		//제외된 개체 정보
		if(excludedNEs != null) {
			ByteUtil.writeInt(outputStream, excludedNEs.size());
			for (String s : excludedNEs) {
				ByteUtil.writeString(outputStream, s);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}
		if(excludedNETypes != null) {
			ByteUtil.writeInt(outputStream, excludedNETypes.size());
			for (String s : excludedNETypes) {
				ByteUtil.writeString(outputStream, s);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}

		if(exclusionPatternsUsed != null) {
			ByteUtil.writeInt(outputStream, exclusionPatternsUsed.size());
			for (String s : exclusionPatternsUsed) {
				ByteUtil.writeString(outputStream, s);
			}
		}else{
			ByteUtil.writeInt(outputStream, 0);
		}


	}

	@Override
	public void deserialize(InputStream inputStream) throws IOException {
		jianaResult.deserialize(inputStream);
		ne_total = ByteUtil.readInt(inputStream);

		int size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			pattern = new String[size];
			for (int i = 0; i < size; i++) {
				pattern[i] = ByteUtil.readString(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			pattern_pos = new int[size];
			for (int i = 0; i < size; i++) {
				pattern_pos[i] = ByteUtil.readInt(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			pattern_size = new int[size];
			for (int i = 0; i < size; i++) {
				pattern_size[i] = ByteUtil.readInt(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			pattern_range = new int[size];
			for (int i = 0; i < size; i++) {
				pattern_range[i] = ByteUtil.readInt(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			lexicalArray = new String[size];
			for (int i = 0; i < size; i++) {
				lexicalArray[i] = ByteUtil.readString(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			ne_res = new String[size];
			for (int i = 0; i < size; i++) {
				ne_res[i] = ByteUtil.readString(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			ne_resOrigin = new String[size];
			for (int i = 0; i < size; i++) {
				ne_resOrigin[i] = ByteUtil.readString(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			eoj_res = new int[size];
			for (int i = 0; i < size; i++) {
				eoj_res[i] = ByteUtil.readInt(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			lex = new String[size];
			for (int i = 0; i < size; i++) {
				lex[i] = ByteUtil.readString(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			ploTag = new String[size];
			for (int i = 0; i < size; i++) {
				ploTag[i] = ByteUtil.readString(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			mTag = new String[size];
			for (int i = 0; i < size; i++) {
				mTag[i] = ByteUtil.readString(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			groupNum = new int[size];
			for (int i = 0; i < size; i++) {
				groupNum[i] = ByteUtil.readInt(inputStream);
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			range = new int[size];
			for (int i = 0; i < size; i++) {
				range[i] = ByteUtil.readInt(inputStream);
			}
		}
		correctTypingSentence = ByteUtil.readString(inputStream);

		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			excludedNEs = new ArrayList<>();
			for (int i = 0; i < size; i++) {
				excludedNEs.add(ByteUtil.readString(inputStream));
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			excludedNETypes = new ArrayList<>();
			for (int i = 0; i < size; i++) {
				excludedNETypes.add(ByteUtil.readString(inputStream));
			}
		}
		size = ByteUtil.readInt(inputStream);
		if(size > 0) {
			exclusionPatternsUsed = new ArrayList<>();
			for (int i = 0; i < size; i++) {
				exclusionPatternsUsed.add(ByteUtil.readString(inputStream));
			}
		}

	}

	public int getNe_total() {
		return ne_total;
	}
} // end of class.
