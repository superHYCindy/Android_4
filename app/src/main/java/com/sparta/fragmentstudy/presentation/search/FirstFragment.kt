package com.sparta.fragmentstudy.presentation.search

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sparta.fragmentstudy.data.database.FavoriteUserRoomDatabase
import com.sparta.fragmentstudy.databinding.FragmentSearchBinding
import com.sparta.fragmentstudy.SpartaApplication
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FirstFragment : Fragment() {
    companion object {
        fun newInstance() = FirstFragment()
    }

    private val searchListAdapter: SearchListAdapter by lazy {
        SearchListAdapter { user ->
            val favoriteUser = user.copy(isFavorite = true)
            //Case 1) Use Listnener
            /*val favoriteUser = user.copy(isFavorite = true)
            likeUserEvent?.likeUser(favoriteUser)*/
            //Case 2) Use Room
            //insertFavoriteUser(user)
            //Case 3) Use MVVM
            //searchViewModel.saveFavoriteUser(favoriteUser)
            //Case 4) Use SharedViewModel
            sharedViewModel.setFavoriteList(favoriteUser)
        }
    }

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    //해당 ViewModel을 초기화하는 Component의 LifeCycle을 따름
    private val searchViewModel by viewModels<SearchViewModel> {
        SearchViewModelFactory((requireActivity()?.application as SpartaApplication).database)
    }

    //activityViewModels : root Activity의 Lifecycle을 따름
    private val sharedViewModel: FavoriteUserSharedViewModel by activityViewModels()

    private var likeUserEvent: LikeUserEvent? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initViewModel()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        likeUserEvent = context as LikeUserEvent
    }

    private fun initViewModel() = with(searchViewModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            searchUiState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collectLatest {
                    when (it) {
                        is UiState.Error -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(
                                requireContext(),
                                it.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is UiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.searchRecyclerview.isVisible = false
                        }

                        is UiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.searchRecyclerview.isVisible = true
                            searchListAdapter.submitList(it.data)
                        }
                    }
                }
        }
    }


    private fun initView() = with(binding) {
        searchRecyclerview.adapter = searchListAdapter
        editQuery.doAfterTextChanged { query ->
            searchViewModel.getImageList(query.toString())
        }
    }

    //TODO : View에서 Room 호출하기
    private fun insertFavoriteUser(user: User) {
        viewLifecycleOwner.lifecycleScope.launch {
            val userDb =
                FavoriteUserRoomDatabase.getDatabase((requireActivity()?.application as SpartaApplication).applicationContext)
            userDb.userDao().insertFavoriteUser(user)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
