package cn.interestingshop.web.client;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cn.interestingshop.entity.BaseOrder;
import cn.interestingshop.entity.Goods;
import cn.interestingshop.entity.User;
import cn.interestingshop.entity.UserAddress;
import cn.interestingshop.service.goods.ClassifyService;
import cn.interestingshop.service.goods.ClassifyServiceImpl;
import cn.interestingshop.service.goods.GoodsService;
import cn.interestingshop.service.goods.GoodsServiceImpl;
import cn.interestingshop.service.order.CartService;
import cn.interestingshop.service.order.CartServiceImpl;
import cn.interestingshop.service.order.OrderService;
import cn.interestingshop.service.order.OrderServiceImpl;
import cn.interestingshop.service.user.UserAddressService;
import cn.interestingshop.service.user.UserAddressServiceImpl;
import cn.interestingshop.utils.ClassifyVo;
import cn.interestingshop.utils.Constants;
import cn.interestingshop.utils.EmptyUtils;
import cn.interestingshop.utils.ReturnResult;
import cn.interestingshop.utils.ShopCart;
import cn.interestingshop.utils.ShopGoods;
import cn.interestingshop.web.AbstractServlet;

/**
 * Created by bdqn 2016/5/3.
 */
@WebServlet(urlPatterns = {"/cart"}, name = "cart")
public class CartServlet extends AbstractServlet {

    private GoodsService goodsService;

    private OrderService orderService;

    private ClassifyService classifyService;

    private CartService cartService;

    private UserAddressService userAddressService;

    public void init() throws ServletException {
        goodsService = new GoodsServiceImpl();
        orderService = new OrderServiceImpl();
        classifyService = new ClassifyServiceImpl();
        cartService = new CartServiceImpl();
        userAddressService = new UserAddressServiceImpl();
    }

    @Override
    public Class getServletClass() {
        return CartServlet.class;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    public ReturnResult add(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ReturnResult result = new ReturnResult();
        String id = request.getParameter("entityId");
        String buyNumStr = request.getParameter("buyNum");
        Integer buyNum = 1;
        if (!EmptyUtils.isEmpty(buyNumStr))
            buyNum = Integer.parseInt(buyNumStr);
        //???????????????
        Goods goods = goodsService.getById(Integer.valueOf(id));
        if(goods.getStock()<buyNum){
        	return result.returnFail("??????????????????");
        }
        //???????????????
        ShopCart cart = getCartFromSession(request);
        //????????????????????????
        result=cart.addShopGoods(goods, buyNum);
        if(result.getStatus()==Constants.ReturnResult.SUCCESS){
        	cart.setSum((EmptyUtils.isEmpty(cart.getSum()) ? 0.0 : cart.getSum()) + (goods.getPrice() * buyNum * 1.0));
        }
        return result;
    }
    
    

    /**
     * ???????????????
     *
     * @param request
     * @param response
     * @return
     */
    public String refreshCart(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        ShopCart cart = getCartFromSession(request);
        cart = cartService.calculate(cart);
        session.setAttribute("cart", cart);//???????????????
        return "/common/client/searchBar";
    }

    /**
     * ??????????????????
     *
     * @param request
     * @param response
     * @return
     */
    public String toSettlement(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<ClassifyVo> classifyVoList = classifyService.getList();
        //????????????
        request.setAttribute("classifyVoList", classifyVoList);
        return "/client/settlement/toSettlement";
    }

    /**
     * ????????????????????????
     *
     * @param request
     * @param response
     * @return
     */
    public String settlement1(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	ShopCart cart = getCartFromSession(request);
    	cart = cartService.calculate(cart);
    	request.getSession().setAttribute("cart",cart);
    	return "/client/settlement/settlement1";
    }

    /**
     * @param request
     * @param response
     * @return
     */
    public String settlement2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = getUserFromSession(request);
        List<UserAddress> userAddressList = userAddressService.getList(user.getId());
        request.setAttribute("userAddressList", userAddressList);
        return "/client/settlement/settlement2";
    }

    /**
     * ????????????
     *
     * @param request
     * @param response
     * @return
     */
    public Object settlement3(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ShopCart cart = getCartFromSession(request);
        cart = cartService.calculate(cart);
        User user = getUserFromSession(request);
        ReturnResult result=checkCart(request);
        if(result.getStatus()==Constants.ReturnResult.FAIL){
        	return result;
        }
        //????????????
        String addressId=request.getParameter("addressId");
        String newAddress=request.getParameter("newAddress");
        String newRemark=request.getParameter("newRemark");
        UserAddress userAddress=new UserAddress();
        if(addressId.equals("-1")){
            userAddress.setRemark(newRemark);
            userAddress.setAddress(newAddress);
            userAddress.setId(userAddressService.save(user.getId(),newAddress,newRemark));
        }else{
            userAddress=userAddressService.getById(Integer.parseInt(addressId));
        }
        
        //????????????
        BaseOrder baseOrder = orderService.pay(user,cart,userAddress.getAddress());
        clearCart(request, response);
        request.setAttribute("currentOrder", baseOrder);
        return "/client/settlement/settlement3";
    }

    /**
     * ???????????????
     *
     * @param request
     * @param response
     */
    public ReturnResult clearCart(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ReturnResult result = new ReturnResult();
        //????????????????????????
        request.getSession().removeAttribute("cart");
        result.returnSuccess(null);
        return result;
    }

    /**
     * ?????????????????????
     *
     * @param request
     * @return
     */
    public ReturnResult modCart(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	ReturnResult result = new ReturnResult();
    	HttpSession session = request.getSession();
        String id = request.getParameter("entityId");
        String buyNumStr = request.getParameter("buyNum");
        ShopCart cart = getCartFromSession(request);
    	Goods goods=goodsService.getById(Integer.valueOf(id));
    	if(EmptyUtils.isNotEmpty(buyNumStr)){
    		if(Integer.parseInt(buyNumStr)>goods.getStock()){
    			return result.returnFail("??????????????????");
        	}
    	}
        cart = cartService.update(id, buyNumStr, cart);
        session.setAttribute("cart", cart);//???????????????
        return result.returnSuccess();
    }

    /**
     * ???session??????????????????
     *
     * @param request
     * @return
     */
    private ShopCart getCartFromSession(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        ShopCart cart = (ShopCart) session.getAttribute("cart");
        if (cart == null) {
            cart = new ShopCart();
            session.setAttribute("cart", cart);
        }
        return cart;
    }
    
    private ReturnResult checkCart(HttpServletRequest request) throws Exception {
    	ReturnResult result = new ReturnResult();
    	ShopCart cart = getCartFromSession(request);
    	cart = cartService.calculate(cart);
    	for (ShopGoods item : cart.getShopGoodsList()) {
           Goods goods=goodsService.getById(item.getGoods().getId());
           if(goods.getStock()<item.getBuyNum()){
        	   return result.returnFail(goods.getGoodsName()+"??????????????????");
           }
        }
    	return result.returnSuccess();
    }

    /**
     * @param request
     * @return
     */
    private User getUserFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("loginUser");
        return user;
    }
}
