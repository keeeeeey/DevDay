import React from 'react';
import { AiOutlineSetting } from 'react-icons/ai';

import Image from 'next/image';

import Footer from '@/components/Footer';
import { SelectArrow } from '@/components/SelectArrow';
import { SelectOption } from '@/components/SelectOption';
import { UserAvatar } from '@/components/UserAvatar';

const index = () => {
  //   const user = http.get(PROFILE_URL, {
  //     Authorization: localStorage.getItem('accessToken'),
  //   });

  return (
    <div>
      <div className="style.div-header sticky top-0 p-4 flex justify-between items-center">
        <h1 className="font-medium text-2xl">마이페이지</h1>
        <div className="rounded-full bg-purple-300 w-10 h-10 flex justify-center items-center">
          <AiOutlineSetting className="text-2xl" />
        </div>
      </div>
      <div className="div-body p-4 relative">
        <div className="absolute top-5 left-7">
          <UserAvatar width={50} height={50} />
        </div>
        <SelectArrow title="nickname" fill />
        <div className="px-6 py-8">
          <div className="flex justify-between mb-6">
            <div className="flex items-center">
              <div>
                <Image
                  src={require('../../image/money.png')}
                  className="aspect-auto w-6 mr-2"
                />
              </div>
              <p>예치금</p>
            </div>
            <p>0원</p>
          </div>
          <div className="flex justify-between mb-4">
            <div className="flex items-center">
              <Image
                src={require('../../image/reward.png')}
                className="aspect-auto w-6 mr-2"
              />
              <p>예치금</p>
            </div>
            <p>0원</p>
          </div>
        </div>
      </div>
      <div>
        <p className="px-4">챌린지 현황</p>
        <hr className="w-full" />
        <div className="flex justify-between my-5 mx-12">
          <div>
            <p>참가중</p>
          </div>
          <div>
            <p>완료</p>
          </div>
          <div>
            <p>리더</p>
          </div>
        </div>
        <hr />
      </div>
      <div className="div-body p-4">
        <SelectOption content="챌린지 개설하기" />
        <SelectOption content="문의하기" />
      </div>
      <div className="absolute w-full bottom-0 m-0">
        <Footer />
      </div>
    </div>
  );
};

export default index;